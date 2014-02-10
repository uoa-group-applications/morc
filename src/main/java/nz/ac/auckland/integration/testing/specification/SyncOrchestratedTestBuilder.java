package nz.ac.auckland.integration.testing.specification;

import nz.ac.auckland.integration.testing.predicate.HeadersPredicate;
import nz.ac.auckland.integration.testing.processor.BodyProcessor;
import nz.ac.auckland.integration.testing.processor.HeadersProcessor;
import nz.ac.auckland.integration.testing.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A builder that generates a synchronous orchestrated test specification that will call a target endpoint
 * that provides a response(s). During the request process the target may make a number of call outs to expectations
 * which need to be satisfied. The response bodies from the target will also be validated against the expected
 * response bodies.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class SyncOrchestratedTestBuilder extends OrchestratedTestSpecification.OrchestratedTestSpecificationBuilder<SyncOrchestratedTestBuilder> {

    private static final Logger logger = LoggerFactory.getLogger(SyncOrchestratedTestBuilder.class);

    private List<TestResource> inputRequestBodies = new ArrayList<>();
    private List<Map<String, Object>> inputRequestHeaders = new ArrayList<>();
    private List<Predicate> responseBodyPredicates = new ArrayList<>();
    private List<HeadersPredicate> responseHeadersPredicates = new ArrayList<>();

    /**
     * @param description The description that identifies what the test is supposed to do
     * @param endpointUri The endpoint URI of the target service under testing
     */
    public SyncOrchestratedTestBuilder(String description, String endpointUri) {
        super(description, endpointUri);
    }

    /**
     * @param resources A collection of test resources that can be used to send request bodies to a target endpoint -
     *                  each body will be placed together with the corresponding requestHeader if available
     */
    public SyncOrchestratedTestBuilder requestBody(TestResource... resources) {
        Collections.addAll(inputRequestBodies, resources);
        return self();
    }

    /**
     * @param resources A collection of test header resources that can be used to send request headers to a target endpoint -
     *                  headers will be placed together with the corresponding requestBody if available
     */
    @SafeVarargs
    public final SyncOrchestratedTestBuilder requestHeaders(Map<String, Object>... resources) {
        Collections.addAll(inputRequestHeaders, resources);
        return self();
    }

    /**
     * @param resources A collection of test header resources that can be used to send request headers to a target endpoint -
     *                  headers will be placed together with the corresponding requestBody if available
     */
    @SafeVarargs
    public final SyncOrchestratedTestBuilder requestHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            try {
                inputRequestHeaders.add(resource.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return self();
    }

    /**
     * @param predicates The set of response validators/predicates that will be used to validate consecutive responses
     */
    public SyncOrchestratedTestBuilder expectedResponse(Predicate... predicates) {
        return this.expectedResponseBody(predicates);
    }

    /**
     * @param predicates The set of response body predicates that will be used to validate consecutive responses - these
     *                   will be paired with the corresponding header predicate
     *
     */
    public SyncOrchestratedTestBuilder expectedResponseBody(Predicate... predicates) {
        Collections.addAll(this.responseBodyPredicates, predicates);
        return self();
    }

    /**
     * @param responseHeadersPredicates The set of response header predicates that will be used to validate consecutive
     *                                  responses - these will be paired with the corresponding body predicate
     */
    public SyncOrchestratedTestBuilder expectedResponseHeaders(HeadersPredicate... responseHeadersPredicates) {
        Collections.addAll(this.responseHeadersPredicates, responseHeadersPredicates);
        return self();
    }

    /**
     * @param resources The set of response header test resources that will be used to validate consecutive
     *                  responses - these will be paired with the corresponding body predicate
     */
    @SafeVarargs
    public final SyncOrchestratedTestBuilder expectedResponseHeaders(TestResource<Map<String, Object>>... resources) {
        for (TestResource<Map<String, Object>> resource : resources) {
            this.responseHeadersPredicates.add(new HeadersPredicate(resource));
        }
        return self();
    }

    @Override
    public OrchestratedTestSpecification build() {
        logger.debug("The endpoint {} will receive {} request message bodies, {} request message headers, " +
                "{} expected response body predicates, and {} expected response headers predicate",
                new Object[]{getEndpointUri(), inputRequestBodies.size(), inputRequestHeaders.size(),
                        responseBodyPredicates.size(), responseHeadersPredicates.size()});

        addRepeatedProcessor(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
            }
        });

        int messageCount = Math.max(inputRequestBodies.size(), inputRequestHeaders.size());
        for (int i = 0; i < messageCount; i++) {
            if (i < inputRequestBodies.size()) {
                try {
                    addProcessors(i, new BodyProcessor(inputRequestBodies.get(i).getValue()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

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
