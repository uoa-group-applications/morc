package nz.ac.auckland.integration.testing;

import nz.ac.auckland.integration.testing.predicate.MultiPredicate;
import nz.ac.auckland.integration.testing.processor.MultiProcessor;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MorcBuilder<Builder extends MorcBuilder<Builder>>  {

    //todo: add endpoint uri here?

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
        List<Processor> finalProcessors = new ArrayList<>();

        if (expectedSize < processors.size()) {
            //discard additional processors (+ warn) - this hasn't been done!
            while (processors.size() > expectedSize) {
                processors.remove(processors.size()-1);
            }
        }

        if (expectedSize > processors.size())
            //warn

        for (int i = 0; i < processors.size(); i++) {
            List<Processor> orderedProcessors = new ArrayList<>(processors.get(i));
            orderedProcessors.addAll(repeatedProcessors);
            finalProcessors.add(new MultiProcessor(orderedProcessors));
        }

        for (int i = expectedSize-1; i < processors.size(); i++) {
            //this may well be empty
            finalProcessors.add(new MultiProcessor(repeatedProcessors));
        }

        return Collections.unmodifiableList(finalProcessors);
    }

    protected List<Predicate> getPredicates() {
        return getPredicates(predicates.size());
    }

    protected List<Predicate> getPredicates(int expectedSize) {
        List<Predicate> finalPredicates = new ArrayList<>();

        if (expectedSize < predicates.size())
            //warn

        if (expectedSize > predicates.size())
            //pad out the predicates (include repeated predicates)

        for (int i = 0; i < predicates.size(); i++) {
            List<Predicate> orderedPredicates = new ArrayList<>(predicates.get(i));
            orderedPredicates.addAll(repeatedPredicates);
            finalPredicates.add(new MultiPredicate(orderedPredicates));
        }

        for (int i = expectedSize - 1; i < predicates.size(); i++) {
            finalPredicates.add(new MultiPredicate(repeatedPredicates));
        }

        return Collections.unmodifiableList(finalPredicates);
    }

    protected Builder self() {
        return (Builder) this;
    }
}
