package nz.ac.auckland.morc;

import nz.ac.auckland.morc.endpointoverride.EndpointOverride;
import nz.ac.auckland.morc.mock.MockDefinition;
import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import org.apache.camel.*;
import org.apache.camel.component.dataset.DataSet;
import org.apache.camel.component.dataset.DataSetComponent;
import org.apache.camel.component.dataset.DataSetEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.TryDefinition;
import org.apache.camel.model.language.ConstantExpression;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * This carries out the actual testing of the orchestrated specification specification - ensuring
 * ordering of the received exchanges is as expected
 * This will be extended for actual tests and will use JUnit Parameterized to add parameters at runtime.
 *
 * @author David MacDonald - d.macdonald@auckland.ac.nz
 */

@Ignore
public class MorcTest extends CamelSpringTestSupport {

    private String[] springContextPaths = new String[]{};
    private String propertiesLocationPath;
    private OrchestratedTestSpecification specification;
    private static final Logger logger = LoggerFactory.getLogger(MorcTest.class);

    public MorcTest() {
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
            throw new IllegalArgumentException("A specification must be set in order to run an orchestrated test");

        logger.info("Starting the test for specification: {} which consists of {} parts", specification.getDescription(),
                specification.getPartCount());

        OrchestratedTestSpecification currentPart = specification;
        int partCount = 1;
        do {
            //there are some instances where we want to delay running parts of the test
            Thread.sleep(currentPart.getExecuteDelay().delay());
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
        MockEndpoint orderCheckMock = context.getEndpoint("mock:" + UUID.randomUUID(), MockEndpoint.class);
        orderCheckMock.expectedMessageCount(spec.getTotalMockMessageCount());
        final MockEndpoint sendingMockEndpoint = context.getEndpoint("mock:responses-" + UUID.randomUUID(), MockEndpoint.class);

        Map<MockEndpoint, MockDefinition> mockEndpointMap = new HashMap<>();

        try {
            Collection<MockDefinition> mockDefinitions = spec.getMockDefinitions();
            logger.trace("Setting up {} mock definitions for the test {}", mockDefinitions.size(), spec.getDescription());

            for (final MockDefinition mockDefinition : mockDefinitions) {
                final MockEndpoint mockEndpoint = context.getEndpoint("mock:" + UUID.randomUUID(), MockEndpoint.class);
                logger.trace("Obtained new mock: {}", mockEndpoint.getEndpointUri());
                mockEndpoints.add(mockEndpoint);
                mockEndpointMap.put(mockEndpoint, mockDefinition);

                mockEndpoint.setExpectedMessageCount(mockDefinition.getExpectedMessageCount());
                logger.trace("Mock for endpoint {} has {} expected messages",
                        mockDefinition.getEndpointUri(), mockDefinition.getExpectedMessageCount());

                for (int i = 0; i < mockDefinition.getProcessors().size(); i++)
                    mockEndpoint.whenExchangeReceived(i + 1, mockDefinition.getProcessors().get(i));

                //result wait time is the *maximum* amount of time we'll wait for all messages
                mockEndpoint.setResultWaitTime(mockDefinition.getResultWaitTime());

                //assert period is the time we'll wait before rechecking everything is fine (i.e. no more messages)
                mockEndpoint.setAssertPeriod(mockDefinition.getReassertionPeriod());

                if (mockDefinition.isEndpointOrdered())
                    mockEndpoint.expectedMessagesMatches(mockDefinition.getPredicates()
                            .toArray(new Predicate[mockDefinition.getPredicates().size()]));
                else
                    mockEndpoint.expects(new Runnable() {
                        public void run() {
                            //need to use a new copy each time for the case of re-assertion
                            ArrayList<Predicate> predicatesCopy = new ArrayList<>(mockDefinition.getPredicates());
                            for (Exchange exchange : mockEndpoint.getExchanges())
                                assertTrue("Message " + exchange + " was received but not matched against a predicate " +
                                        "on endpoint " + mockDefinition.getEndpointUri(), remove(predicatesCopy, exchange));
                        }

                        private boolean remove(ArrayList<Predicate> predicates, Exchange exchange) {
                            for (Iterator<Predicate> iterator = predicates.iterator(); iterator.hasNext(); ) {
                                if (iterator.next().matches(exchange)) {
                                    iterator.remove();
                                    return true;
                                }
                            }
                            return false;
                        }
                    });

                //this will be the route used to receive the message and validate/process the exchange
                RouteDefinition mockRouteDefinition = new RouteDefinition();
                mockRouteDefinition.from(mockDefinition.getEndpointUri())
                        .convertBodyTo(byte[].class)
                        .routeId(MorcTest.class.getCanonicalName() + "." + mockDefinition.getEndpointUri())
                        .setProperty("endpointUri", new ConstantExpression(mockDefinition.getEndpointUri()))
                        .log(LoggingLevel.DEBUG, "Endpoint ${property.endpointUri} received body: ${body}, headers: ${headers}");

                ProcessorDefinition pd = mockRouteDefinition;

                if (mockDefinition.getLenientSelector() != null)
                    pd = mockRouteDefinition
                            .choice()
                            .when(mockDefinition.getLenientSelector())
                            .log(LoggingLevel.INFO, "Endpoint ${property.endpointUri} received a message for lenient processing")
                            .process(mockDefinition.getLenientProcessor())
                            .endChoice()
                            .otherwise();

                pd.wireTap(orderCheckMock.getEndpointUri())
                        .log(LoggingLevel.INFO, "Endpoint ${property.endpointUri} received a message");

                if (mockDefinition.getMockFeedPreprocessor() != null)
                    pd.process(mockDefinition.getMockFeedPreprocessor());

                pd.to(mockEndpoint)
                        .log(LoggingLevel.DEBUG, "Endpoint ${property.endpointUri} returning back to the client body: ${body}, headers: ${headers}")
                        .end();

                Endpoint targetEndpoint = getMandatoryEndpoint(mockDefinition.getEndpointUri());
                for (EndpointOverride override : mockDefinition.getEndpointOverrides())
                    override.overrideEndpoint(targetEndpoint);

                context.addRouteDefinition(mockRouteDefinition);
                createdRoutes.add(mockRouteDefinition);
            }

            //set up sending messages to the target system under testing
            sendingMockEndpoint.expectedMessageCount(spec.getProcessors().size());
            sendingMockEndpoint.expectedMessagesMatches(spec.getPredicates().toArray(new Predicate[spec.getPredicates().size()]));

            //setup the route for sending messages
            DataSetComponent component = new DataSetComponent();
            component.setCamelContext(context);

            //apply endpoint overrides to the producer endpoint
            Endpoint targetEndpoint = getMandatoryEndpoint(spec.getEndpointUri());
            for (EndpointOverride override : spec.getEndpointOverrides())
                override.overrideEndpoint(targetEndpoint);

            //a latch to check whether message publishing has completed
            final CountDownLatch latch = new CountDownLatch(spec.getProcessors().size());

            MessagePublishDataSet dataSet = new MessagePublishDataSet(spec.getProcessors());
            DataSetEndpoint dataSetEndpoint = new DataSetEndpoint("dataset:" + UUID.randomUUID(), component, dataSet);
            dataSetEndpoint.setProduceDelay(spec.getSendInterval());

            RouteDefinition publishRouteDefinition = new RouteDefinition();

            //ensure we have completed sending each exchange
            sendingMockEndpoint.whenAnyExchangeReceived(exchange -> latch.countDown());

            TryDefinition tryDefinition = publishRouteDefinition.from(dataSetEndpoint)
                    .routeId(MorcTest.class.getCanonicalName() + ".publish")
                    .log(LoggingLevel.DEBUG, "Sending to endpoint " + spec.getEndpointUri() + " body: ${body}, headers: ${headers}")
                    .handleFault()
                    .doTry(); //for some reason onException().continued(true) doesn't work

            if (spec.getTestBean() == null) tryDefinition.to(targetEndpoint);
            else tryDefinition.process(spec.getTestBean());

            tryDefinition
                    .convertBodyTo(byte[].class)
                    .doCatch(Throwable.class).end()
                    .choice().when(property(Exchange.EXCEPTION_CAUGHT).isNotNull())
                    .log(LoggingLevel.DEBUG, "Received exception response to endpoint " + spec.getEndpointUri()
                            + " exception: ${exception}, headers: ${headers}")
                    .otherwise()
                    .log(LoggingLevel.DEBUG, "Received response from endpoint " + spec.getEndpointUri()
                            + " body: ${body}, headers: ${headers}")
                    .end();

            if (spec.getMockFeedPreprocessor() != null) tryDefinition.process(spec.getMockFeedPreprocessor());

            tryDefinition.to(sendingMockEndpoint);

            context.addRouteDefinition(publishRouteDefinition);

            createdRoutes.add(publishRouteDefinition);

            //wait until we have sent all messages
            logger.trace("Starting wait for all messages to be published");
            latch.await();
            logger.trace("Messages have all been published");

            try {
                logger.trace("Starting sending mock endpoint assertion");
                sendingMockEndpoint.assertIsSatisfied();
            } catch (AssertionError e) {
                throw new AssertionError("The target endpoint " + spec.getEndpointUri() + " on test " +
                        spec.getDescription() + " provided an " + "invalid response: " + e.getMessage(), e);
            }
            logger.debug("Completion of message publishing with response validation was successful");

            for (MockEndpoint mockEndpoint : mockEndpoints) {
                logger.trace("Starting mock assertion for endpoint {}", mockEndpoint.getEndpointUri());
                try {
                    mockEndpoint.assertIsSatisfied();
                } catch (AssertionError e) {
                    throw new AssertionError("Mock expectation for endpoint: " + mockEndpointMap.get(mockEndpoint).getEndpointUri() +
                            " failed validation: " + e.getMessage() + " for test " + spec.getDescription(), e);
                }
                logger.debug("Successfully completed mock assertion for endpoint {}", mockEndpoint.getEndpointUri());
            }

            //we know that all messages will have arrived by this point therefore we are unconcerned with wait/assertion times
            logger.trace("Starting assertion for ordering checking");
            try {
                orderCheckMock.assertIsSatisfied();
            } catch (AssertionError e) {
                throw new AssertionError("The total number of expected messages did not arrive at the mock services for test " +
                        spec.getDescription(), e);
            }
            logger.debug("Successfully validated that all messages arrive to endpoints in the correct order");

            //We now need to check that messages have arrived in the correct order
            Collection<OrchestratedTestSpecification.EndpointNode> endpointNodes = new ArrayList<>(spec.getEndpointNodesOrdering());
            for (Exchange e : orderCheckMock.getExchanges()) {
                OrchestratedTestSpecification.EndpointNode node = findEndpointNodeMatch(endpointNodes, e.getFromEndpoint());

                StringBuilder expectedNodeEndpoints = new StringBuilder();
                for (OrchestratedTestSpecification.EndpointNode endpointNode : endpointNodes) {
                    expectedNodeEndpoints.append(endpointNode.getEndpointUri()).append(",");
                }

                String expectedNodeEndpointsOutput = expectedNodeEndpoints.toString();
                if (expectedNodeEndpointsOutput.length() > 0)
                    expectedNodeEndpointsOutput = expectedNodeEndpointsOutput.substring(0, expectedNodeEndpointsOutput.length() - 1);

                logger.trace("Expected arrivals to endpoints {}", expectedNodeEndpointsOutput);

                //this means we don't expect to have seen the message at this point
                assertNotNull("A message to the endpoint " + e.getFromEndpoint().getEndpointUri() +
                        " was unexpected - one of " + expectedNodeEndpointsOutput + " was expected for test " +
                        spec.getDescription(), node);

                //we've encountered a message to this endpoint and should remove it from the set
                endpointNodes.remove(node);
                endpointNodes.addAll(node.getChildrenNodes());
            }

            logger.debug("Successfully validated that messages arrived to endpoints in the correct order");

        } finally {
            for (RouteDefinition routeDefinition : createdRoutes)
                context.removeRouteDefinition(routeDefinition);

            sendingMockEndpoint.reset();

            for (MockEndpoint mockEndpoint : mockEndpoints)
                mockEndpoint.reset();

            orderCheckMock.reset();
        }
    }

    private OrchestratedTestSpecification.EndpointNode findEndpointNodeMatch(Collection<OrchestratedTestSpecification.EndpointNode> endpointNodes, Endpoint endpoint) {
        for (OrchestratedTestSpecification.EndpointNode node : endpointNodes) {
            if (endpoint.equals(context.getEndpoint(node.getEndpointUri()))) {
                logger.debug("Message arrived in the correct order to endpoint {}", node.getEndpointUri());
                return node;
            }
        }
        //not found, which should cause an exception
        return null;
    }
}

class MessagePublishDataSet implements DataSet {

    private static final Logger logger = LoggerFactory.getLogger(MessagePublishDataSet.class);

    private List<Processor> processors;

    public MessagePublishDataSet(List<Processor> processors) {
        this.processors = processors;
    }

    @Override
    public void populateMessage(Exchange exchange, long messageIndex) throws Exception {
        logger.trace("Sending message {}", messageIndex);
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
