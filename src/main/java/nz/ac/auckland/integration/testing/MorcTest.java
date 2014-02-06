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
import org.apache.camel.model.language.ConstantExpression;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        MockEndpoint orderCheckMock = context.getEndpoint("mock:" + UUID.randomUUID(), MockEndpoint.class);
        orderCheckMock.expectedMessageCount(spec.getTotalMockMessageCount());

        try {
            Collection<MockDefinition> mockDefinitions = spec.getMockDefinitions();

            //set up the mocks
            for (final MockDefinition mockDefinition : mockDefinitions) {
                final MockEndpoint mockEndpoint = context.getEndpoint("mock:" + UUID.randomUUID(), MockEndpoint.class);
                logger.debug("Obtained new mock: {}", mockEndpoint.getEndpointUri());
                mockEndpoints.add(mockEndpoint);

                //why do we need i+1??
                mockEndpoint.setExpectedMessageCount(mockDefinition.getExpectedMessageCount());
                for (int i = 0; i < mockDefinition.getProcessors().size(); i++)
                    mockEndpoint.whenExchangeReceived(i + 1, mockDefinition.getProcessors().get(i));

                //todo clear this up - assert times
                mockEndpoint.setSleepForEmptyTest(mockDefinition.getAssertionTime());
                mockEndpoint.setResultWaitTime(mockDefinition.getAssertionTime());
                mockEndpoint.setAssertPeriod(mockDefinition.getAssertionTime());

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


                if (mockDefinition.getLenientSelector() != null) {
                    mockDefinition.getMockFeederRoute()
                            .choice()
                            .when(mockDefinition.getLenientSelector())
                            .log(LoggingLevel.DEBUG, "Endpoint ${routeId} received message for lenient processing")
                            .process(mockDefinition.getLenientProcessor())
                            .otherwise();
                }

                mockDefinition.getMockFeederRoute()
                        .routeId(MorcTest.class.getCanonicalName() + "." + mockDefinition.getEndpointUri())
                        .setProperty("endpointUri", new ConstantExpression(mockDefinition.getEndpointUri()))
                        .wireTap(orderCheckMock.getEndpointUri())
                        .log(LoggingLevel.INFO, "Endpoint ${property.endpointUri} received a message")
                        .log(LoggingLevel.DEBUG, "Endpoint ${property.endpointUri} received body: ${body}, headers: ${headers}")
                        .to(mockEndpoint)
                        .onCompletion()
                        .log(LoggingLevel.DEBUG, "Endpoint ${property.endpointUri} returning back to client body: ${body}, headers: ${headers}");

                Endpoint targetEndpoint = getMandatoryEndpoint(mockDefinition.getEndpointUri());
                for (EndpointOverride override : mockDefinition.getEndpointOverrides())
                    override.overrideEndpoint(targetEndpoint);

                context.addRouteDefinition(mockDefinition.getMockFeederRoute());
                createdRoutes.add(mockDefinition.getMockFeederRoute());
            }

            //set up sending messages to the target system under testing
            final MockEndpoint sendingMockEndpoint = context.getEndpoint("mock:responses-" + UUID.randomUUID(), MockEndpoint.class);
            mockEndpoints.add(sendingMockEndpoint);
            sendingMockEndpoint.expectedMessageCount(spec.getProcessors().size());
            sendingMockEndpoint.expectedMessagesMatches(spec.getPredicates().toArray(new Predicate[spec.getPredicates().size()]));

            RouteDefinition publishRouteDefinition = new RouteDefinition();

            //setup the route for sending messages
            DataSetComponent component = new DataSetComponent();
            component.setCamelContext(context);

            //apply endpoint overrides to the producer endpoint
            Endpoint targetEndpoint = getMandatoryEndpoint(spec.getEndpointUri());
            for (EndpointOverride override : spec.getEndpointOverrides())
                override.overrideEndpoint(targetEndpoint);

            final CountDownLatch processedMessageLatch = new CountDownLatch(spec.getTotalPublishMessageCount());

            publishRouteDefinition.from(new DataSetEndpoint("dataset:" + UUID.randomUUID(), component,
                    new MessagePublishDataSet(spec.getProcessors())))
                    .routeId(MorcTest.class.getCanonicalName() + ".publish")
                    .handleFault()
                    .onCompletion()
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                processedMessageLatch.countDown();
                            }
                        })
                    .end()
                    .log(LoggingLevel.DEBUG, "Sending to endpoint " + spec.getEndpointUri() + " body: ${body}, headers: ${headers}")
                    .doTry() //for some reason onException().continued(true) doesn't work
                        .to(targetEndpoint)
                    .doCatch(Throwable.class).end()
                    .choice().when(property(Exchange.EXCEPTION_CAUGHT).isNotNull())
                    .log(LoggingLevel.INFO, "Received exception response to endpoint " + spec.getEndpointUri()
                            + " exception: ${exception}, headers: ${headers}")
                    .otherwise()
                    .log(LoggingLevel.INFO, "Received response to endpoint " + spec.getEndpointUri()
                            + " body: ${body}, headers: ${headers}")
                    .end()
                    .to(sendingMockEndpoint);

            context.addRouteDefinition(publishRouteDefinition);

            createdRoutes.add(publishRouteDefinition);

            logger.trace("Starting sending mock endpoint assertion");
            //extra 5s is to give some time for route to boot
            sendingMockEndpoint.setResultWaitTime(5000l + spec.getMessageAssertTime() * (spec.getTotalPublishMessageCount()+1));
            sendingMockEndpoint.assertIsSatisfied();
            logger.trace("Completed sending mock endpoint assertion");

            for (MockEndpoint mockEndpoint : mockEndpoints)
                mockEndpoint.assertIsSatisfied();

            orderCheckMock.assertIsSatisfied();

            //We now need to check that messages have arrived in the correct order
            Collection<OrchestratedTestSpecification.EndpointNode> endpointNodes = new ArrayList<>(spec.getEndpointNodesOrdering());
            for (Exchange e : orderCheckMock.getExchanges()) {
                OrchestratedTestSpecification.EndpointNode node = findEndpointNodeMatch(endpointNodes, e.getFromEndpoint());

                //this means we don't expect to have seen the message at this point
                assertNotNull("add useful exception message here", node);

                //we've encountered a message to this endpoint and should remove it from the set
                endpointNodes.remove(node);
                endpointNodes.addAll(node.getChildrenNodes());
            }

        } finally {
            for (RouteDefinition routeDefinition : createdRoutes)
                context.removeRouteDefinition(routeDefinition);

            for (MockEndpoint mockEndpoint : mockEndpoints)
                mockEndpoint.reset();

            orderCheckMock.reset();
        }
    }

    private OrchestratedTestSpecification.EndpointNode findEndpointNodeMatch(Collection<OrchestratedTestSpecification.EndpointNode> endpointNodes, Endpoint endpoint) {
        for (OrchestratedTestSpecification.EndpointNode node : endpointNodes) {
            if (endpoint.equals(context.getEndpoint(node.getEndpointUri()))) return node;
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



