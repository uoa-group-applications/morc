package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.processor.HeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A synchronous orchestrated test makes a call to a target endpoint which provides a response. During
 * the request process the target may make a number of call outs to expectations which need to be satisfied.
 * The response body from the target will also be validated against the expected response body.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncOrchestratedTestBuilder extends OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder<SyncOrchestratedTestBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(SyncOrchestratedTestBuilder.class);

    private List<TestResource> inputRequestBodies = new ArrayList<>();
    private List<Map<String, Object>> inputRequestHeaders = new ArrayList<>();
    private List<Predicate> responseBodyPredicates = new ArrayList<>();
    private List<HeadersPredicate> responseHeadersPredicates = new ArrayList<>();

    public SyncOrchestratedTestBuilder(String description, String endpointUri) {
        super(description, endpointUri);
    }

    /**
     * @param resources A collection of test resources that can be used to send request bodies to a target endpoint
     */
    public SyncOrchestratedTestBuilder requestBody(TestResource... resources) {
        Collections.addAll(inputRequestBodies, resources);
        return self();
    }

    /**
     * @param resources A collection of test header resources that can be used to send request headers to a target endpoint
     */
    @SafeVarargs
    public final SyncOrchestratedTestBuilder requestHeaders(Map<String, Object>... resources) {
        Collections.addAll(inputRequestHeaders, resources);
        return self();
    }

    public SyncOrchestratedTestBuilder expectedResponseBody(Predicate... predicates) {
        Collections.addAll(this.responseBodyPredicates, predicates);
        return self();
    }

    public SyncOrchestratedTestBuilder expectedResponseHeaders(HeadersPredicate... responseHeadersPredicates) {
        Collections.addAll(this.responseHeadersPredicates, responseHeadersPredicates);
        return self();
    }

    @SafeVarargs
    public final SyncOrchestratedTestBuilder expectedResponseHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            this.responseHeadersPredicates.add(new HeadersPredicate(resource));
        }
        return self();
    }

    @Override
    public OrchestratedTestSpecification build() {
        logger.info("The endpoint {} will be sending {} request message bodies, {} request message headers, " +
                "{} expected response body predicates, and {} expected response headers predicate",
                new Object[]{getEndpointUri(), inputRequestBodies.size(), inputRequestHeaders.size(),
                        responseBodyPredicates.size(), responseHeadersPredicates.size()});

        int messageCount = Math.max(inputRequestBodies.size(), inputRequestHeaders.size());
        for (int i = 0; i < messageCount; i++) {
            if (i < inputRequestBodies.size())
                addProcessors(i, new BodyProcessor(inputRequestBodies.get(i)));

            if (i < inputRequestHeaders.size())
                addProcessors(i, new HeadersProcessor(inputRequestHeaders.get(i)));
        }

        int responseCount = Math.max(responseBodyPredicates.size(), responseHeadersPredicates.size());
        for (int i = 0; i < responseCount; i++) {
            if (i < responseBodyPredicates.size())
                addPredicates(i, responseBodyPredicates.get(i));

            if (i < responseHeadersPredicates.size())
                addPredicates(i, responseHeadersPredicates.get(i));
        }

        return super.build();
    }

}
