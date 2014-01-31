package nz.ac.auckland.integration.testing;

import nz.ac.auckland.integration.testing.endpointoverride.CxfEndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.endpointoverride.UrlConnectionOverride;
import nz.ac.auckland.integration.testing.predicate.MultiPredicate;
import nz.ac.auckland.integration.testing.processor.MultiProcessor;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;

public class MorcBuilder<Builder extends MorcBuilder<Builder>> {

    private String endpointUri;
    private static final Logger logger = LoggerFactory.getLogger(MorcBuilder.class);

    private List<List<Processor>> processors = new ArrayList<>();
    private List<List<Predicate>> predicates = new ArrayList<>();

    private List<Processor> repeatedProcessors = new ArrayList<>();
    private List<Predicate> repeatedPredicates = new ArrayList<>();

    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();

    public MorcBuilder(String endpointUri) {
        try {
            this.endpointUri = URISupport.normalizeUri(endpointUri);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        //we don't want to use POJO to receive messages
        endpointOverrides.add(new CxfEndpointOverride());
        endpointOverrides.add(new UrlConnectionOverride());
    }

    public Builder addProcessors(Processor... processors) {
        this.processors.add(Arrays.asList(processors));
        return self();
    }

    public Builder addProcessors(int index, Processor... processors) {
        while (index >= this.processors.size()) {
            this.processors.add(new ArrayList<Processor>());
        }
        this.processors.get(index).addAll(Arrays.asList(processors));
        return self();
    }

    public Builder addRepeatedProcessor(Processor processor) {
        repeatedProcessors.add(processor);
        return self();
    }

    public Builder addPredicates(Predicate... predicates) {
        this.predicates.add(Arrays.asList(predicates));
        return self();
    }

    public Builder addPredicates(int index, Predicate... predicates) {
        while (index >= this.predicates.size()) {
            this.predicates.add(new ArrayList<Predicate>());
        }
        this.predicates.get(index).addAll(Arrays.asList(predicates));
        return self();
    }

    public Builder addRepeatedPredicate(Predicate predicate) {
        repeatedPredicates.add(predicate);
        return self();
    }

    protected List<Processor> getProcessors() {
        return getProcessors(processors.size());
    }

    protected List<Processor> getProcessors(int expectedSize) {
        List<List<Processor>> localProcessors = new ArrayList<>(processors);
        List<Processor> finalProcessors = new ArrayList<>();

        if (expectedSize < localProcessors.size()) {
            logger.warn("The endpoint uri {} has been provided with more processors than there are expected messages; " +
                    "the remainder will be removed", endpointUri);
            while (localProcessors.size() > expectedSize) {
                localProcessors.remove(localProcessors.size() - 1);
            }
        }

        if (expectedSize > localProcessors.size() && repeatedPredicates.size() == 0)
            logger.warn("The endpoint uri {} has fewer processors than there are expected messages; " +
                    "nothing will happen to these messages when they arrive", endpointUri);

        for (List<Processor> localProcessor : localProcessors) {
            List<Processor> orderedProcessors = new ArrayList<>(localProcessor);
            orderedProcessors.addAll(repeatedProcessors);
            finalProcessors.add(new MultiProcessor(orderedProcessors));
        }

        for (int i = expectedSize; i < localProcessors.size(); i++) {
            //this may well be empty
            finalProcessors.add(new MultiProcessor(repeatedProcessors));
        }

        return finalProcessors;
    }

    protected List<Predicate> getPredicates() {
        return getPredicates(predicates.size());
    }

    protected List<Predicate> getPredicates(int expectedSize) {
        List<List<Predicate>> localPredicates = new ArrayList<>(predicates);
        List<Predicate> finalPredicates = new ArrayList<>();

        if (expectedSize > localPredicates.size() && repeatedPredicates.size() != 0)
            logger.warn("The endpoint uri {} has more messages expected than provided predicates; subsequent messages " +
                    "will be accepted without any validation", endpointUri);

        for (List<Predicate> localPredicate : localPredicates) {
            List<Predicate> orderedPredicates = new ArrayList<>(localPredicate);
            orderedPredicates.addAll(repeatedPredicates);
            finalPredicates.add(new MultiPredicate(orderedPredicates));
        }

        for (int i = expectedSize; i < localPredicates.size(); i++) {
            finalPredicates.add(new MultiPredicate(repeatedPredicates));
        }

        return finalPredicates;
    }

    /**
     * @param override An override used for modifying an endpoint for *receiving* a message
     */
    public Builder addEndpointOverride(EndpointOverride override) {
        endpointOverrides.add(override);
        return self();
    }

    /**
     * @return The endpoint overrides that modify the sending endpoint
     */
    public Collection<EndpointOverride> getEndpointOverrides() {
        return Collections.unmodifiableCollection(this.endpointOverrides);
    }

    public String getEndpointUri() {
        return this.endpointUri;
    }

    protected Builder self() {
        return (Builder) this;
    }
}
