package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.resource.TestResource;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import org.apache.camel.Predicate;
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
public class ContentMockDefinitionBuilder<Builder extends MockDefinition.MockDefinitionBuilder<Builder>>
        extends MockDefinition.MockDefinitionBuilder<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ContentMockDefinitionBuilder.class);

    List<Predicate> expectedBodyPredicates = new ArrayList<>();
    List<HeadersPredicate> expectedHeadersPredicates = new ArrayList<>();

    @Override
    protected Builder self() {
        return (Builder)this;
    }

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public ContentMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param validators A list of validators that check the body is as expected; each validator should match
     *                   to a headers validator, and an expectation will be generated for each pair
     */
    public Builder expectedBody(Predicate... validators) {
        Collections.addAll(expectedBodyPredicates, validators);
        return self();
    }

    /**
     * @param validators An list of validators that check the headers are as expected; each validator should match
     *                   to a body validator, and an expectation will be generated for each pair
     */
    public Builder expectedHeaders(HeadersPredicate... validators) {
        Collections.addAll(expectedHeadersPredicates, validators);
        return self();
    }

    /**
     * @param resources A list of resources will match the expected headers; each resource should match
     *                  to a body validator, and an expectation will be generated for each pair
     */
    @SafeVarargs
    public final Builder expectedHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            expectedHeadersPredicates.add(new HeadersPredicate(resource));
        }
        return self();
    }

    @Override
    public MockDefinition build(MockDefinition previousExpectationPart) {

        int validatorSize = Math.max(expectedBodyPredicates.size(), expectedHeadersPredicates.size());

        logger.info("{} body validators, and {} header validators provided for endpoint {}",
                new Object[]{expectedBodyPredicates.size(), expectedHeadersPredicates.size(), getEndpointUri()});

        for (int i = 0; i < validatorSize; i++) {

            Predicate expectedBodyPredicate = null;
            HeadersPredicate expectedHeadersPredicate = null;

            //because null validators are accepted we don't have to set them
            if (i < expectedBodyPredicates.size())
                expectedBodyPredicate = expectedBodyPredicates.get(i);
            if (i < expectedHeadersPredicates.size())
                expectedHeadersPredicate = expectedHeadersPredicates.get(i);

            this.addPredicates(i, expectedBodyPredicate, expectedHeadersPredicate);

        }

        return super.build(previousExpectationPart);
    }

}
