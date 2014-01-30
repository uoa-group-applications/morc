package nz.ac.auckland.integration.testing;

import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import org.apache.camel.*;
import org.apache.camel.component.dataset.DataSet;
import org.apache.camel.component.dataset.DataSetComponent;
import org.apache.camel.component.dataset.DataSetEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.support.LifecycleStrategySupport;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This carries out the actual testing of the orchestrated specification specification - ensuring
 * ordering of the received exchanges is as expected
 * <p/>
 * This will be extended for actual tests and will use JUnit Parameterized to add parameters at runtime.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */

public class MorcTest extends CamelSpringTestSupport {

    private String[] springContextPaths = new String[]{};
    private String propertiesLocationPath;
    private OrchestratedTestSpecification specification;
    private static final Logger logger = LoggerFactory.getLogger(MorcTest.class);

    protected MorcTest() {
        configureXmlUnit();
    }

    /**
     * @param specification A test specification that this test runs
     */
    public MorcTest(OrchestratedTestSpecification specification) {
        this();
        this.specification = specification;
    }

    public MorcTest(OrchestratedTestSpecification specification, String[] springContextPaths) {
        this(specification);
        this.springContextPaths = springContextPaths;
    }

    public MorcTest(OrchestratedTestSpecification specification, String propertiesLocationPath) {
        this(specification);
        this.propertiesLocationPath = propertiesLocationPath;
    }

    public MorcTest(OrchestratedTestSpecification specification, String[] springContextPaths, String propertiesLocationPath) {
        this(specification, springContextPaths);
        this.propertiesLocationPath = propertiesLocationPath;
    }

    //I would've prefered to use a constructor-only approach but sub-classing makes this infeasible
    protected void setSpecification(OrchestratedTestSpecification specification) {
        this.specification = specification;
    }

    /**
     * Override this to return a list of Spring context paths on the classpath
     *
     * @return An array of classpath Spring XML file references
     */
    public String[] getSpringContextPaths() {
        return springContextPaths;
    }

    /**
     * Override this to return a path to a properties file for managing Camel endpoint URIs
     *
     * @return A string path to a properties file
     */
    public String getPropertiesLocation() {
        return propertiesLocationPath;
    }

    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(getSpringContextPaths());
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        String propertiesLocation = getPropertiesLocation();
        if (propertiesLocation != null) {
            PropertiesComponent properties = new PropertiesComponent();
            properties.setLocation(propertiesLocation);
            context.addComponent("properties", properties);
        }

        return context;
    }

    /**
     * Configure XML Unit parameters for comparing XML - override this to adjust the defaults
     */
    protected void configureXmlUnit() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void runOrchestratedTest() throws Exception {

        if (specification == null)
            throw new IllegalStateException("A specification must be set in order to run an orchestrated test");

        logger.info("Starting the test for specification: {} which consists of {} parts", specification.getDescription(),
                specification.getPartCount());

        OrchestratedTestSpecification currentPart = specification;
        int partCount = 1;
        do {
            logger.debug("Starting test specification {} part {}", specification.getDescription(), partCount);
            runSpecificationPart(currentPart);
            logger.info("Successfully completed test specification {} part {}", specification.getDescription(), partCount);

            currentPart = currentPart.getNextPart();
            partCount++;
        } while (currentPart != null);

        logger.info("Successfully completed the specification: " + specification.getDescription());
    }

    private void runSpecificationPart(final OrchestratedTestSpecification spec) throws Exception {

        Set<MockEndpoint> mockEndpoints = new HashSet<>();
        Set<RouteDefinition> createdRoutes = new HashSet<>();

        try {
            Set<MockDefinition> mockDefinitions = spec.getMockDefinitions();

            MockEndpoint orderCheckMock = context.getEndpoint("mock:" + UUID.randomUUID(),MockEndpoint.class);
            orderCheckMock.expectedMessageCount(spec.getTotalMessageCount());

            //set up the mocks
            for (final MockDefinition mockDefinition : mockDefinitions) {
                final MockEndpoint mockEndpoint = context.getEndpoint("mock:" + UUID.randomUUID(), MockEndpoint.class);
                logger.debug("Obtained new mock: {}", mockEndpoint.getEndpointUri());
                mockEndpoints.add(mockEndpoint);

                mockEndpoint.setExpectedMessageCount(mockDefinition.getExpectedMessageCount());
                for (int i = 0; i < mockDefinition.getProcessors().size(); i++)
                    mockEndpoint.whenExchangeReceived(i,mockDefinition.getProcessors().get(i));

                //todo clear this up - assert times
                mockEndpoint.setSleepForEmptyTest(mockDefinition.getAssertionTime());
                mockEndpoint.setResultWaitTime(mockDefinition.getAssertionTime());
                mockEndpoint.setAssertPeriod(mockDefinition.getAssertionTime());

                if (mockDefinition.isEndpointOrdered())
                    mockEndpoint.expectedMessagesMatches(mockDefinition.getPredicates()
                            .toArray(new Predicate[mockDefinition.getPredicates().size()]));
                else {
                    final List<Predicate> equalsPredicatesList = new ArrayList<>();
                    for (final Predicate predicate : mockDefinition.getPredicates()) {
                        equalsPredicatesList.add(new Predicate() {
                            @Override
                            public boolean matches(Exchange exchange) {
                                return predicate.matches(exchange);
                            }

                            @Override
                            public boolean equals(Object object) {
                                return (object instanceof Exchange) && predicate.matches((Exchange) object);
                            }
                        });
                    }

                    mockEndpoint.expects(new Runnable() {
                        public void run() {
                            List<Predicate> equalsPredicatesListCopy = new ArrayList<>(equalsPredicatesList);
                            for (Exchange exchange : mockEndpoint.getExchanges())
                                assertTrue("Message " + exchange + " was received but not matched against a predicate " +
                                        "on endpoint " + mockDefinition.getEndpointUri(), equalsPredicatesListCopy.remove(exchange));
                        }
                    });
                }

                if (mockDefinition.getLenientSelector() != null) {
                    mockDefinition.getMockFeederRoute()
                            .choice()
                            .when(mockDefinition.getLenientSelector())
                            .log(LoggingLevel.DEBUG,"Endpoint ${routeId} received message for lenient processing")
                            .process(mockDefinition.getLenientProcessor())
                            .otherwise();
                }

                mockDefinition.getMockFeederRoute()
                        .routeId(mockDefinition.getEndpointUri())
                        .wireTap(orderCheckMock.getEndpointUri())
                        .log(LoggingLevel.DEBUG, "Endpoint ${routeId} received body: ${body}, headers: ${headers}")
                        .to(mockEndpoint)
                        .onCompletion()
                        .log(LoggingLevel.DEBUG,"Endpoint ${routeId} returning back to client body: ${body}, headers: ${headers}");

                Endpoint targetEndpoint = getMandatoryEndpoint(mockDefinition.getEndpointUri());
                for (EndpointOverride override : mockDefinition.getEndpointOverrides())
                    override.overrideEndpoint(targetEndpoint);

                context.addRouteDefinition(mockDefinition.getMockFeederRoute());
                createdRoutes.add(mockDefinition.getMockFeederRoute());
            }

            //set up sending messages to the target system under testing
            final MockEndpoint sendingMockEndpoint = context.getEndpoint("mock:" + UUID.randomUUID(), MockEndpoint.class);
            mockEndpoints.add(sendingMockEndpoint);
            sendingMockEndpoint.expectedMessageCount(spec.getProcessors().size());
            sendingMockEndpoint.expectedMessagesMatches(spec.getPredicates().toArray(new Predicate[spec.getPredicates().size()]));
            for (int i = 0; i < spec.getProcessors().size(); i++)
                sendingMockEndpoint.whenExchangeReceived(i,spec.getProcessors().get(i));

            RouteDefinition publishRouteDefinition = new RouteDefinition();

            publishRouteDefinition.from(new DataSetEndpoint("dataset:" + UUID.randomUUID(),new DataSetComponent(),
                    new MessagePublishDataSet(spec.getProcessors())))
                .log(LoggingLevel.DEBUG,"Sending to endpoint " + spec.getEndpointUri() + " body: ${body}, headers: ${headers}")
                .to(spec.getEndpointUri())
                .onCompletion()
                    .log(LoggingLevel.DEBUG,"Received response to endpoint " + spec.getEndpointOverrides() + " body: ${body}, headers: ${headers}")
                    .to(sendingMockEndpoint)
                    .executorService(context.getExecutorServiceManager().newSingleThreadExecutor(this,spec.getEndpointUri()))
                .end();

            createdRoutes.add(publishRouteDefinition);

            Endpoint targetEndpoint = getMandatoryEndpoint(spec.getEndpointUri());
            for (EndpointOverride override : spec.getEndpointOverrides())
                    override.overrideEndpoint(targetEndpoint);

            sendingMockEndpoint.assertIsSatisfied();

            for (MockEndpoint mockEndpoint : mockEndpoints)
                    mockEndpoint.assertIsSatisfied();

            orderCheckMock.assertIsSatisfied();

            //We now need to check that messages have arrived in the correct order
            Collection<OrchestratedTestSpecification.EndpointNode> endpointNodes = new ArrayList<>(spec.getEndpointNodesOrdering());
            for (Exchange e : orderCheckMock.getExchanges()) {
                OrchestratedTestSpecification.EndpointNode node = findEndpointNodeMatch(endpointNodes,e.getFromEndpoint().getEndpointUri());

                //this means we don't expect to have seen the message at this point
                if (node == null)
                    throw new AssertionError("");

                //we've encountered a message to this endpoint and should remove it from the set
                endpointNodes.remove(node);
                endpointNodes.addAll(node.getChildrenNodes());
            }

        } finally {
            for (RouteDefinition routeDefinition : createdRoutes)
                context.removeRouteDefinition(routeDefinition);

            for (MockEndpoint mockEndpoint : mockEndpoints)
                mockEndpoint.reset();
        }
    }

    private OrchestratedTestSpecification.EndpointNode findEndpointNodeMatch(Collection<OrchestratedTestSpecification.EndpointNode> endpointNodes, String endpointUri) {
        for (OrchestratedTestSpecification.EndpointNode node : endpointNodes) {
            if (node.getEndpointUri().equals(endpointUri)) return node;
        }

        //not found, which should cause an exception
        return null;
    }
}

class MessagePublishDataSet implements DataSet {

    private List<Processor> processors;

    public MessagePublishDataSet(List<Processor> processors) {
        this.processors = processors;
    }

    @Override
    public void populateMessage(Exchange exchange, long messageIndex) throws Exception {
        processors.get((int) messageIndex).process(exchange);
    }
    @Override
    public long getSize() {
        return processors.size();
    }
    @Override
    public void assertMessageExpected(DataSetEndpoint endpoint, Exchange expected, Exchange actual, long messageIndex) throws Exception {
        throw new UnsupportedOperationException();
    }
    @Override
    public long getReportCount() {
        return processors.size();
    }
}



