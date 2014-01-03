package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.validator.HeadersValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A class to set expectations for bodies and headers for
 * an incoming message
 * <p/>
 * This class carries out the message validation based on the test resource, expectations
 * will be returned in the order specified even if some relaxation of total ordering occurs
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ContentMockExpectationBuilder<Builder extends MockExpectation.MockExpectationBuilder<Builder>>
        extends MockExpectation.MockExpectationBuilder<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ContentMockExpectationBuilder.class);

    List<Validator> expectedBodyValidators = new ArrayList<>();
    List<HeadersValidator> expectedHeadersValidators = new ArrayList<>();

    @Override
    protected Builder self() {
        return (Builder)this;
    }

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public ContentMockExpectationBuilder(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param validators A list of validators that check the body is as expected; each validator should match
     *                   to a headers validator, and an expectation will be generated for each pair
     */
    public Builder expectedBody(Validator... validators) {
        Collections.addAll(expectedBodyValidators, validators);
        return self();
    }

    /**
     * @param validators An list of validators that check the headers are as expected; each validator should match
     *                   to a body validator, and an expectation will be generated for each pair
     */
    public Builder expectedHeaders(HeadersValidator... validators) {
        Collections.addAll(expectedHeadersValidators, validators);
        return self();
    }

    /**
     * @param resources A list of resources will match the expected headers; each resource should match
     *                  to a body validator, and an expectation will be generated for each pair
     */
    @SafeVarargs
    public final Builder expectedHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            expectedHeadersValidators.add(new HeadersValidator(resource));
        }
        return self();
    }

    @Override
    public MockExpectation build(MockExpectation previousExpectationPart, int index) {

        int validatorSize = Math.max(expectedBodyValidators.size(),expectedHeadersValidators.size());

        logger.info("{} body validators, and {} header validators provided for endpoint {}",
                new Object[] {expectedBodyValidators.size(),expectedHeadersValidators.size(), getEndpointUri()});

        for (int i = 0; i < validatorSize; i++) {

            Validator expectedBodyValidator = null;
            HeadersValidator expectedHeadersValidator = null;

            //because null validators are accepted we don't have to set them
            if (i < expectedBodyValidators.size())
                expectedBodyValidator = expectedBodyValidators.get(i);
            if (i < expectedHeadersValidators.size())
                expectedHeadersValidator = expectedHeadersValidators.get(i);

            this.addValidators(i,expectedBodyValidator,expectedHeadersValidator);

        }

        return super.build(previousExpectationPart,index);
    }

}
