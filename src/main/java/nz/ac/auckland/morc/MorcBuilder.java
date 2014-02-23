package nz.ac.auckland.morc;

import nz.ac.auckland.morc.endpointoverride.CxfEndpointOverride;
import nz.ac.auckland.morc.endpointoverride.EndpointOverride;
import nz.ac.auckland.morc.endpointoverride.UrlConnectionOverride;
import nz.ac.auckland.morc.predicate.MultiPredicate;
import nz.ac.auckland.morc.processor.MultiProcessor;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * The root builder for specifying processors and predicates for message publishers and mock definitions
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MorcBuilder<Builder extends MorcBuilder<Builder>> {

    private String endpointUri;
    private static final Logger logger = LoggerFactory.getLogger(MorcBuilder.class);

    private List<List<Processor>> processors = new ArrayList<>();
    private List<List<Predicate>> predicates = new ArrayList<>();

    private List<Processor> repeatedProcessors = new ArrayList<>();
    private List<Predicate> repeatedPredicates = new ArrayList<>();

    private long messageResultWaitTime = 1000l;
    private long minimalResultWaitTime = 10000l;
    private Processor mockFeedPreprocessor;

    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();

    /**
     * @param endpointUri he endpoint URI that this definition expects to act against
     */
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

    /**
     * Adds processors for populating a series of exchanges with an outgoing message - all processors in a single call
     * apply ONLY to a single message, add consecutive calls to addProcessors in order to handle further messages
     *
     * @param processors A list of processors that will handle a separate exchange (in order)
     */
    public Builder addProcessors(Processor... processors) {
        this.processors.add(new ArrayList<>(Arrays.asList(processors)));
        return self();
    }

    /**
     * Add a set of processors to handle an outgoing exchange at a particular offset (n'th message)
     *
     * @param index      The exchange offset that these processors should be applied to
     * @param processors The processors that will handle populating the exchange with an appropriate outgoing value
     */
    public Builder addProcessors(int index, Processor... processors) {
        while (index >= this.processors.size()) {
            this.processors.add(new ArrayList<Processor>());
        }
        this.processors.get(index).addAll(new ArrayList<>(Arrays.asList(processors)));
        return self();
    }

    /**
     * @param processor A processor that will be applied to every outgoing message
     */
    public Builder addRepeatedProcessor(Processor processor) {
        repeatedProcessors.add(processor);
        return self();
    }

    /**
     * Add a set of predicates to validate an incoming exchange - all predicates in a single call
     * apply ONLY to a single message, add consecutive calls to addPredicates in order to handle further messages
     *
     * @param predicates A list of predicates that will validate a separate exchange (in order)
     */
    public Builder addPredicates(Predicate... predicates) {
        this.predicates.add(new ArrayList<>(Arrays.asList(predicates)));
        return self();
    }

    /**
     * Add a set of predicates to validate an exchange at a particular offset (n'th message)
     *
     * @param index      The exchange offset that these predicates should be validated against
     * @param predicates The set of predicates that will do the validation of the exchange
     */
    public Builder addPredicates(int index, Predicate... predicates) {
        while (index >= this.predicates.size()) {
            this.predicates.add(new ArrayList<Predicate>());
        }
        this.predicates.get(index).addAll(new ArrayList<>(Arrays.asList(predicates)));
        return self();
    }

    /**
     * @param predicate A predicate that will be used to validate every exchange
     */
    public Builder addRepeatedPredicate(Predicate predicate) {
        repeatedPredicates.add(predicate);
        return self();
    }

    /**
     * @return A list of processors that will be used to handle each exchange; note that a single Processor is returned
     *         that effectively wraps all of the processors provided to the builder (including repeated processors)
     */
    protected List<Processor> getProcessors() {
        return getProcessors(processors.size());
    }

    /**
     * @param expectedSize The number of processors that we expect to exist, and the collection will be padded to this size
     * @return A list of processors that will be used to handle each exchange; note that a single Processor is returned
     *         that effectively wraps all of the processors provided to the builder (including repeated processors)
     */
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

        while (finalProcessors.size() < expectedSize) {
            //this may well be empty
            finalProcessors.add(new MultiProcessor(repeatedProcessors));
        }

        return finalProcessors;
    }

    /**
     * @return A list of predicates that will be used to validate each exchange; note that a single Predicate is returned
     *         that effectively wraps all of the predicates provided to the builder (including repeated predicates)
     */
    protected List<Predicate> getPredicates() {
        return getPredicates(predicates.size());
    }

    /**
     * @param expectedSize The number of predicates that we expect to exist, and the collection will be padded to this size
     * @return A list of predicates that will be used to validate each exchange; note that a Predicate is returned
     *         that effectively wraps all of the predicates provided to the builder (including repeated predicates)
     */
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

        while (finalPredicates.size() < expectedSize) {
            finalPredicates.add(new MultiPredicate(repeatedPredicates));
        }

        return finalPredicates;
    }

    /**
     * @param override An override used for modifying an endpoint with sensible properties
     */
    public Builder addEndpointOverride(EndpointOverride override) {
        //skip the ones we're already aware of
        if (override instanceof CxfEndpointOverride || override instanceof UrlConnectionOverride) return self();
        endpointOverrides.add(override);
        return self();
    }

    /**
     * @return The endpoint overrides that will be used to modify endpoint properties
     */
    public Collection<EndpointOverride> getEndpointOverrides() {
        return Collections.unmodifiableCollection(this.endpointOverrides);
    }

    /**
     * @param messageResultWaitTime The maximum amount of time in milliseconds per message that the test will wait for
     *                              a result to be provided (this will be multiplied by the expected number of messages
     *                              to give a maximum wait time for all messages to be handled)
     */
    public Builder messageResultWaitTime(long messageResultWaitTime) {
        this.messageResultWaitTime = messageResultWaitTime;
        return self();
    }

    /**
     * @return The maximum amount of time in milliseconds per message that the test will wait for
     *         a result to be provided (this will be multiplied by the expected number of messages
     *         to give a maximum wait time for all messages to be handled)
     */
    public long getMessageResultWaitTime() {
        return messageResultWaitTime;
    }

    /**
     * @param minimalResultWaitTime The minimum time in milliseconds for forming a maximum time that a test will wait
     *                              for all messages to arrive/complete. This is usually used as a buffer time for
     *                              ensuring a route has started before the messageResultWaitTime values are accounted
     *                              for
     */
    public Builder minimalResultWaitTime(long minimalResultWaitTime) {
        this.minimalResultWaitTime = minimalResultWaitTime;
        return self();
    }

    /**
     * @return The minimum time in milliseconds for forming a maximum time that a test will wait
     *         for all messages to arrive/complete. This is usually used as a buffer time for
     *         ensuring a route has started before the messageResultWaitTime values are accounted for
     */
    public long getMinimalResultWaitTime() {
        return minimalResultWaitTime;
    }

    /**
     * @return The endpoint URI that this definition expects to act against
     */
    public String getEndpointUri() {
        return this.endpointUri;
    }

    /**
     * @return A processor that will be applied before the exchange is sent through to the mock endpoint
     */
    public Processor getMockFeedPreprocessor() {
        return mockFeedPreprocessor;
    }

    /**
     * @param mockFeedPreprocessor A processor that will be applied before the exchange is sent through to the mock endpoint
     */
    public Builder mockFeedPreprocessor(Processor mockFeedPreprocessor) {
        this.mockFeedPreprocessor = mockFeedPreprocessor;
        return self();
    }

    @SuppressWarnings("unchecked")
    protected Builder self() {
        return (Builder) this;
    }
}
