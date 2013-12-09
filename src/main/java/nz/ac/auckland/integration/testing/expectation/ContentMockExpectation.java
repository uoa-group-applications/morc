package nz.ac.auckland.integration.testing.expectation;

import nz.ac.auckland.integration.testing.resource.*;
import nz.ac.auckland.integration.testing.validator.*;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An abstract class to set expectations for bodies and headers for
 * an incoming message
 * <p/>
 * This class carries out the message validation based on the test resource, expectations
 * will be returned in the order specified even if some relaxation of total ordering occurs
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public abstract class ContentMockExpectation extends MockExpectation {
    private List<Validator> expectedBodyValidators;
    private List<HeadersValidator> expectedHeadersValidators;

    private static final Logger logger = LoggerFactory.getLogger(ContentMockExpectation.class);

    /**
     * @return The expected headers that the target endpoint expects to receive at a particular point in test
     */
    public List<HeadersValidator> getExpectedHeadersValidators() {
        return Collections.unmodifiableList(expectedHeadersValidators);
    }

    /**
     * @return The expected body that the target endpoint expects to receive at a particular point in the test
     */
    public List<Validator> getExpectedBodyValidators() {
        return Collections.unmodifiableList(expectedBodyValidators);
    }

    /**
     * Evaluates whether the content coming in from an endpoint meets the content requirements. Ordering checks
     * are delegated to the superclass.
     *
     * Synchronized so that the body and headers validator can be retrieved in lock-step
     */
    public synchronized boolean checkValid(Exchange incomingExchange, int index) {
        if (!super.checkValid(incomingExchange, index)) return false;

        int validatorOffset = index - getReceivedAt();

        //now check the expected bodies and headers are valid
        boolean validateBody = validatorOffset >= expectedBodyValidators.size() ||
                expectedBodyValidators.get(validatorOffset).validate(incomingExchange);
        boolean validateHeaders = validatorOffset >= expectedHeadersValidators.size() ||
                expectedHeadersValidators.get(validatorOffset).validate(incomingExchange);

        return validateBody && validateHeaders;
    }

    public static abstract class AbstractContentBuilder<Product extends MockExpectation,
            Builder extends AbstractBuilder<Product, Builder>> extends AbstractBuilder<Product, Builder> {

        private List<Validator> expectedBodyValidators = new ArrayList<>();
        private List<HeadersValidator> expectedHeadersValidators = new ArrayList<>();

        public AbstractContentBuilder(String endpointUri) {
            super(endpointUri);
        }

        public Builder expectedBody(Validator... validators) {
            Collections.addAll(expectedBodyValidators, validators);
            return self();
        }

        //todo: this as an enumeration too
        public Builder expectedBody(XmlTestResource... resources) {
            for (XmlTestResource resource : resources) {
                expectedBodyValidators.add(new XmlValidator(resource));
            }

            return self();
        }

        public Builder expectedBody(JsonTestResource... resources) {
            for (JsonTestResource resource : resources) {
                expectedBodyValidators.add(new JsonValidator(resource));
            }
            return self();
        }

        public Builder expectedBody(PlainTextTestResource... resources) {
            for (PlainTextTestResource resource : resources) {
                expectedBodyValidators.add(new PlainTextValidator(resource));
            }
            return self();
        }

        public Builder expectedBody(Enumeration<Validator> validators) {
            while (validators.hasMoreElements()) {
                expectedBodyValidators.add(validators.nextElement());
            }

            return self();
        }

        public Builder expectedHeaders(HeadersValidator... validators) {
            Collections.addAll(expectedHeadersValidators, validators);
            return self();
        }

        @SafeVarargs
        public final Builder expectedHeaders(TestResource<Map<String,Object>>... resources) {
            for (TestResource<Map<String,Object>> resource : resources) {
                expectedHeadersValidators.add(new HeadersValidator(resource));
            }
            return self();
        }

        public Builder expectedHeaders(Enumeration<TestResource<Map<String,Object>>> validators) {
            while (validators.hasMoreElements()) {
                expectedHeadersValidators.add(new HeadersValidator(validators.nextElement()));
            }

            return self();
        }

        protected int expectedMessageCount() {
            if (expectedBodyValidators.size() != expectedHeadersValidators.size())
                logger.warn("A different number of body and header validators were provided for the endpoint %s", endpointUri);

            return Math.max(expectedBodyValidators.size(), expectedHeadersValidators.size());
        }

    }

    @SuppressWarnings("unchecked")
    protected ContentMockExpectation(AbstractContentBuilder builder) {
        super(builder);

        this.expectedBodyValidators = builder.expectedBodyValidators;
        this.expectedHeadersValidators = builder.expectedHeadersValidators;
    }
}
