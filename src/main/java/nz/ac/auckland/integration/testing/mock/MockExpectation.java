package nz.ac.auckland.integration.testing.mock;

import nz.ac.auckland.integration.testing.validator.OrderValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.URISupport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A general class for specifying an expectation for a consumer of the
 * given endpoint URI. Each expectation can have a name (to distinguish
 * between expectations on the same endpoint).
 * <p/>
 * By default messages are expected to be in strict order (they arrive in the
 * order that they are defined in the test). This can be relaxed by setting the
 * orderingType to something other than TOTAL; PARTIAL ordering will allow messages
 * to arrive at a point in time after they're expected (useful for asynchronous messaging)
 * and also NONE which will accept a matching expectation at any point in the test. It is
 * also possible to set endpoint ordering to false such that messages can arrive in any order
 * - this may be useful when you just care about a message arriving, and not providing a
 * meaningful/ordered response.
 * <p/>
 * By default, it is expected that each expectation will occur only
 * once on a given endpoint; by setting expectedMessageCount we can repeat
 * the same expectation multiple times following the ordering parameters above
 * (e.g. by default, an expectedMessageCount of 3 would expect 3 messages in
 * a row without messages arriving from any other endpoint).
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MockExpectation implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MockExpectation.class);

    private String endpointUri;
    private boolean isEndpointOrdered = true;
    private boolean lenient = false;
    private OrderingType orderingType;
    private List<List<Processor>> processors;
    private List<List<Validator>> validators;
    private int expectedMessageCount;
    private AtomicInteger messageCounter = new AtomicInteger();
    private boolean exchangesValid = true;

    public enum OrderingType {
        TOTAL,
        PARTIAL,
        NONE
    }

    /**
     * @return The ordering type this expectation requires to be satisfied; the default is TOTAL
     *         which means it must arrive in the exact order it was specified. PARTIAL means it must
     *         arrive after it was defined. NONE means that it can arrive at any time at all during the
     *         test. The actual test execution will manage these differing ordering requirements between
     *         the different endpoints
     */
    public OrderingType getOrderingType() {
        return orderingType;
    }

    /**
     * @return The Camel-formatted URI that should be listened to for messages and requests
     */
    public String getEndpointUri() {
        return endpointUri;
    }

    /**
     * @return Whether messages arriving at this endpoint have to arrive in order
     */
    public boolean isEndpointOrdered() {
        return isEndpointOrdered;
    }

    public boolean isLenient() {
        return lenient;
    }

    public List<List<Processor>> getProcessors() {
        return processors;
    }

    public List<List<Validator>> getValidators() {
        return validators;
    }

    public int getExpectedMessageCount() {
        return expectedMessageCount;
    }

    protected synchronized void validateExchange(Exchange exchange, int messageIndex) {
        if (!isEndpointOrdered || orderingType != OrderingType.NONE) {
            List<Validator> exchangeValidators = validators.remove(0);
            exchangesValid = exchangesValid && validateExchangeList(exchange,messageIndex,exchangeValidators);
        } else {
            Iterator<List<Validator>> validatorLists = validators.iterator();
            while (validatorLists.hasNext()) {
                exchangesValid = exchangesValid && validateExchangeList(exchange,messageIndex, validatorLists.next());
                if (exchangesValid) {
                    validatorLists.remove();
                    break;
                }
            }
        }
    }

    private boolean validateExchangeList(Exchange exchange, int messageIndex, List<Validator> validators) {
        boolean validExchange = true;

        for (Validator validator : validators) {
            boolean valid = validator.validate(exchange, messageIndex);
            validExchange = valid && validExchange;
        }

        return validExchange;
    }

    public boolean isValid() {
        return (lenient || exchangesValid && validators.size() == 0 && expectedMessageCount == messageCounter.get());
    }

    /**
     * This is what is called by the test once a message has arrived at an endpoint. It is useful in setting the
     * outgoing response in the case of a synchronous expectation
     *
     * @param exchange The Camel exchange that needs to be modified, or handled once it has been received
     * @throws Exception
     */
    public void process(Exchange exchange) throws Exception {
        //todo log incoming message (debug/trace/info)

        int currentEndpointMessageIndex = messageCounter.getAndIncrement();
        int currentOverallMessageIndex = exchange.getProperty("SOAUnitMessageIndex",Integer.class);

        validateExchange(exchange,currentOverallMessageIndex);

        //todo I think i broke math here
        if (currentEndpointMessageIndex >= processors.size() && !isLenient()) {
            logger.warn("A message was received to endpoint {} but the expectation provided no processor for this " +
                    "message",endpointUri);
            return;
        } else {
            currentEndpointMessageIndex = currentEndpointMessageIndex % processors.size();
        }

        for (Processor processor : processors.get(currentEndpointMessageIndex)) {
            processor.process(exchange);
        }
    }

    /*
        Using details from: https://weblogs.java.net/node/642849
     */
    public static class MockExpectationBuilder<Builder extends MockExpectationBuilder<Builder>> {

        private String endpointUri;
        private OrderingType orderingType = OrderingType.TOTAL;
        private boolean isEndpointOrdered = true;
        private boolean lenient = false;
        private List<List<Processor>> processors = new ArrayList<>();
        private List<List<Validator>> validators = new ArrayList<>();
        private int expectedMessageCount = 1;

        private List<Processor> repeatedProcessors = new ArrayList<>();
        private List<Validator> repeatedValidators = new ArrayList<>();

        /**
         * @param endpointUri A Camel Endpoint URI to listen to for expected messages
         */
        public MockExpectationBuilder(String endpointUri) {
            try {
                this.endpointUri = URISupport.normalizeUri(endpointUri);
            } catch (URISyntaxException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder expectedMessageCount(int expectedMessageCount) {
            this.expectedMessageCount = expectedMessageCount;
            return self();
        }

        /**
         * Specifies the ordering that this expectation requires (TOTAL, PARTIAL or NONE)
         */
        public Builder ordering(OrderingType orderingType) {
            this.orderingType = orderingType;
            return self();
        }

        /**
         * Specifies whether the endpoint expects messages in the order that they are defined.
         */
        public Builder endpointNotOrdered() {
            isEndpointOrdered = false;
            return self();
        }

        public Builder lenient() {
            this.lenient = true;
            return self();
        }

        public Builder addProcessors(Processor... processors) {
            this.processors.add(Arrays.asList(processors));
            return self();
        }

        public Builder addProcessors(int index, Processor... processors) {
            while (index > this.processors.size()) {
                this.processors.add(new ArrayList<Processor>());
            }
            this.processors.get(index).addAll(Arrays.asList(processors));
            return self();
        }

        public Builder addRepeatedProcessor(Processor processor) {
            repeatedProcessors.add(processor);
            return self();
        }

        public Builder addValidators(Validator... validators) {
            this.validators.add(Arrays.asList(validators));
            return self();
        }

        public Builder addValidators(int index, Validator... validators) {
            while (index > this.validators.size()) {
                this.validators.add(new ArrayList<Validator>());
            }
            this.validators.get(index).addAll(Arrays.asList(validators));
            return self();
        }

        public Builder addRepeatedValidator(Validator validator) {
            repeatedValidators.add(validator);
            return self();
        }

        public MockExpectation build(MockExpectation previousExpectationPart, int index) {

            if (expectedMessageCount < 0)
                throw new IllegalStateException("The expected message count for the expectation on endpoint "
                        + endpointUri + " must be at least 0");

            expectedMessageCount = Math.max(expectedMessageCount, validators.size());

            //do repeated validators and repeated expectations
            for (int i = 0; i < expectedMessageCount; i++) {
                addProcessors(i,repeatedProcessors.toArray(new Processor[repeatedProcessors.size()]));
                addValidators(i,repeatedValidators.toArray(new Validator[repeatedValidators.size()]));

                //add an order validator
                addValidators(i, new OrderValidator(index+i,orderingType));
            }

            //ensure the number of processors doesn't mess up the next expextation
            if (processors.size() > expectedMessageCount) {
                logger.warn("The endpoint {} has {} expected messages but only {} message processors",
                        new Object[] {endpointUri,validators.size(),processors.size()});

                if (!lenient) {
                    logger.warn("The additional processors for expectation endpoint {} are being removed to match the " +
                            "expected message count",endpointUri);
                    while (processors.size() > expectedMessageCount) {
                        List<Processor> removedProcessors = processors.remove(processors.size()-1);
                        logger.debug("The endpoint {} is having the expectation processors removed: {}",
                                endpointUri,StringUtils.join(removedProcessors,","));
                    }
                }
            }

            if (previousExpectationPart != null) {

                if (!previousExpectationPart.getEndpointUri().equals(endpointUri))
                    throw new IllegalStateException("The endpoint do not much for merging expectation " +
                            previousExpectationPart.getEndpointUri() + " and " + endpointUri);

                if (previousExpectationPart.isEndpointOrdered() != isEndpointOrdered)
                    throw new IllegalStateException("The endpoint ordering must be the same for all expectation parts of " +
                            "endpoint " + endpointUri);

                if (previousExpectationPart.isLenient())
                    throw new IllegalStateException("Only one lenient expectation is allowed for an endpoint: " + getEndpointUri());

                if (previousExpectationPart.getOrderingType() != orderingType)
                    throw new IllegalStateException("The ordering type must be same for all expectations on an endpoint: "
                            + getEndpointUri());

                //prepend the previous validators/processors onto this list ot make an updated expectation
                this.validators.addAll(0, previousExpectationPart.getValidators());
                this.processors.addAll(0, previousExpectationPart.getProcessors());
                this.expectedMessageCount += previousExpectationPart.getExpectedMessageCount();

            }

            return new MockExpectation(this);
        }

        protected Builder self() {
            return (Builder)this;
        }

        protected String getEndpointUri() {
            return this.endpointUri;
        }

        protected List<List<Validator>> getValidators() {
            return Collections.unmodifiableList(validators);
        }

        protected List<List<Processor>> getProcessors() {
            return Collections.unmodifiableList(processors);
        }

        protected OrderingType getOrderingType() {
            return this.orderingType;
        }

    }

    @SuppressWarnings("unchecked")
    protected MockExpectation(MockExpectationBuilder builder) {
        this.endpointUri = builder.endpointUri;
        this.orderingType = builder.orderingType;
        this.isEndpointOrdered = builder.isEndpointOrdered;
        this.lenient = builder.lenient;
        this.processors = builder.processors;
        this.validators = builder.validators;
        this.expectedMessageCount = builder.expectedMessageCount;
    }
}
