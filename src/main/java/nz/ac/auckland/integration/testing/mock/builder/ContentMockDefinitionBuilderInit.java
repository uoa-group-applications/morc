package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class to set expectations for bodies and headers for
 * an incoming message
 * <p/>
 * This class carries out the message validation based on the test resource, expectations
 * will be returned in the order specified even if some relaxation of total ordering occurs
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ContentMockDefinitionBuilderInit<Builder extends ContentMockDefinitionBuilderInit<Builder>>
        extends MockDefinition.MockDefinitionBuilderInit<Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ContentMockDefinitionBuilderInit.class);

    private List<Predicate> expectedBodyPredicates = new ArrayList<>();
    private List<HeadersPredicate> expectedHeadersPredicates = new ArrayList<>();

    @Override
    protected Builder self() {
        return (Builder) this;
    }

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public ContentMockDefinitionBuilderInit(String endpointUri) {
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
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        int validatorSize = Math.max(expectedBodyPredicates.size(), expectedHeadersPredicates.size());

        logger.debug("{} body validators, and {} header validators provided for mock definition endpoint {}",
                new Object[]{expectedBodyPredicates.size(), expectedHeadersPredicates.size(), getEndpointUri()});

        for (int i = 0; i < validatorSize; i++) {
            if (i < expectedBodyPredicates.size())
                this.addPredicates(i, expectedBodyPredicates.get(i));
            if (i < expectedHeadersPredicates.size())
                this.addPredicates(i, expectedHeadersPredicates.get(i));
        }

        return super.build(previousDefinitionPart);
    }

}
