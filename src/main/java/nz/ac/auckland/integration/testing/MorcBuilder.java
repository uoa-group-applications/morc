package nz.ac.auckland.integration.testing;

import nz.ac.auckland.integration.testing.predicate.MultiPredicate;
import nz.ac.auckland.integration.testing.processor.MultiProcessor;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MorcBuilder<Builder extends MorcBuilder<Builder>>  {

    private String endpointUri;
    private static final Logger logger = LoggerFactory.getLogger(MorcBuilder.class);

    public MorcBuilder(String endpointUri) {
        try {
            this.endpointUri = URISupport.normalizeUri(endpointUri);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<List<Processor>> processors = new ArrayList<>();
    private List<List<Predicate>> predicates = new ArrayList<>();

    private List<Processor> repeatedProcessors = new ArrayList<>();
    private List<Predicate> repeatedPredicates = new ArrayList<>();

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

    public Builder addPredicates(Predicate... predicates) {
        this.predicates.add(Arrays.asList(predicates));
        return self();
    }

    public Builder addPredicates(int index, Predicate... predicates) {
        while (index > this.predicates.size()) {
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
                    "the remainder will be removed",endpointUri);
            while (localProcessors.size() > expectedSize) {
                localProcessors.remove(localProcessors.size()-1);
            }
        }

        if (expectedSize > localProcessors.size() && repeatedPredicates.size() == 0)
            logger.warn("The endpoint uri {} has fewer processors than there are expected messages; " +
                    "nothing will happen to these messages when they arrive",endpointUri);

        for (List<Processor> localProcessor : localProcessors) {
            List<Processor> orderedProcessors = new ArrayList<>(localProcessor);
            orderedProcessors.addAll(repeatedProcessors);
            finalProcessors.add(new MultiProcessor(orderedProcessors));
        }

        for (int i = expectedSize-1; i < localProcessors.size(); i++) {
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
                    "will be accepted without any validation",endpointUri);

        for (List<Predicate> localPredicate : localPredicates) {
            List<Predicate> orderedPredicates = new ArrayList<>(localPredicate);
            orderedPredicates.addAll(repeatedPredicates);
            finalPredicates.add(new MultiPredicate(orderedPredicates));
        }

        for (int i = expectedSize - 1; i < localPredicates.size(); i++) {
            finalPredicates.add(new MultiPredicate(repeatedPredicates));
        }

        return finalPredicates;
    }

    public String getEndpointUri() {
        return this.endpointUri;
    }

    protected Builder self() {
        return (Builder) this;
    }
}
