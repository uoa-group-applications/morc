package nz.ac.auckland.morc;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.mock.builder.AsyncMockDefinitionBuilder;
import nz.ac.auckland.morc.mock.builder.SyncMockDefinitionBuilder;
import nz.ac.auckland.morc.mock.builder.UnreceivedMockDefinitionBuilder;
import nz.ac.auckland.morc.specification.AsyncOrchestratedTestBuilder;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import nz.ac.auckland.morc.specification.SyncOrchestratedTestBuilder;
import org.apache.camel.util.URISupport;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(value = MorcParameterized.class)
public abstract class MorcTestBuilder extends MorcTest implements MorcMethods {

    private List<OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit> specificationBuilders = new ArrayList<>();

    protected abstract void configure();

    /**
     * @param endpointUri The endpoint URI that an asynchronous message should be sent to
     * @param description A description for the test specification that clearly identifies it
     * @return An asynchronous test specification builder with the endpoint uri and description configured
     */
    protected AsyncOrchestratedTestBuilder asyncTest(String description, String endpointUri) {
        if (endpointUri.startsWith("http")) endpointUri = "jetty:" + endpointUri;
        AsyncOrchestratedTestBuilder builder = new AsyncOrchestratedTestBuilder(description, endpointUri);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param description A description for the test specification that clearly identifies it
     * @param testBean    Java code to run to kick off the test rather than a Camel URI
     * @return An asynchronous test specification builder with the bean to execute and description configured
     */
    protected AsyncOrchestratedTestBuilder asyncTest(String description, TestBean testBean) {
        AsyncOrchestratedTestBuilder builder = new AsyncOrchestratedTestBuilder(description, testBean);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param endpointUri The endpoint URI that a synchronous message should be sent to
     * @param description A description for the test specification that clearly identifies it
     * @return A synchronous test specification builder with the endpoint uri and description configured
     */
    protected SyncOrchestratedTestBuilder syncTest(String description, String endpointUri) {
        if (endpointUri.startsWith("http")) endpointUri = "jetty:" + endpointUri;
        SyncOrchestratedTestBuilder builder = new SyncOrchestratedTestBuilder(description, endpointUri);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param description A description for the test specification that clearly identifies it
     * @param testBean    Java code to run to kick off the test rather than a Camel URI
     * @return A synchronous test specification builder with the bean to execute and description configured
     */
    protected SyncOrchestratedTestBuilder syncTest(String description, TestBean testBean) {
        SyncOrchestratedTestBuilder builder = new SyncOrchestratedTestBuilder(description, testBean);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public AsyncMockDefinitionBuilder asyncMock(String endpointUri) {
        if (endpointUri.startsWith("http")) endpointUri = "jetty:" + endpointUri;
        return new AsyncMockDefinitionBuilder(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public SyncMockDefinitionBuilder syncMock(String endpointUri) {
        if (endpointUri.startsWith("http")) endpointUri = "jetty:" + endpointUri;
        return new SyncMockDefinitionBuilder(endpointUri);
    }

    public SyncMockDefinitionBuilder restMock(String endpointUri) {
        try {
            if (!URISupport.parseParameters(new URI(endpointUri)).containsKey("matchOnUriPrefix")) {
                Map<String,Object> params = new HashMap<>();
                params.put("matchOnUriPrefix","true");
                endpointUri = URISupport.appendParametersToURI(endpointUri,params);
            }
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return syncMock(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public UnreceivedMockDefinitionBuilder unreceivedMock(String endpointUri) {
        if (endpointUri.startsWith("http")) endpointUri = "jetty:" + endpointUri;
        return new UnreceivedMockDefinitionBuilder(endpointUri);
    }

    /**
     * @return A way of specifying that the next endpoint in the specification list should be synchronous
     */
    public Class<SyncOrchestratedTestBuilder> syncTest() {
        return SyncOrchestratedTestBuilder.class;
    }

    /**
     * @return A way of specifying that the next endpoint in the specification list should be asynchronous
     */
    public Class<AsyncOrchestratedTestBuilder> asyncTest() {
        return AsyncOrchestratedTestBuilder.class;
    }

    /**
     * @return We expect messages to be totally ordered (amongst endpoints)
     */
    public MockDefinition.OrderingType totalOrdering() {
        return MockDefinition.OrderingType.TOTAL;
    }

    /**
     * @return We expect messages to arrive at some point after we expect
     */
    public MockDefinition.OrderingType partialOrdering() {
        return MockDefinition.OrderingType.PARTIAL;
    }

    /**
     * @return We expect messages arrive at any point of the test
     */
    public MockDefinition.OrderingType noOrdering() {
        return MockDefinition.OrderingType.NONE;
    }

    //this is used by JUnit to initialize each instance of this specification
    protected List<OrchestratedTestSpecification> getSpecifications() {
        configure();

        List<OrchestratedTestSpecification> specifications = new ArrayList<>();

        for (OrchestratedTestSpecification.OrchestratedTestSpecificationBuilderInit builder : specificationBuilders) {
            OrchestratedTestSpecification spec = builder.build();
            specifications.add(spec);
        }

        return specifications;
    }

    /**
     * A method to allow tests to be run from simple scripts without all the JUnit infrastructure
     *
     * @return The number of failed tests
     */
    public int run() {
        JoranConfigurator configurator = new JoranConfigurator();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(classpath("/logback-morc.xml"));
        } catch (JoranException e) {
            throw new RuntimeException(e);
        }

        JUnitCore core = new JUnitCore();
        core.addListener(new TextListener(System.out));
        try {
            Result r = core.run(new MorcParameterized(this));
            return r.getFailureCount();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
