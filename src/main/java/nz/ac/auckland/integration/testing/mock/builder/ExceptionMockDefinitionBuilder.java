package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This builder will add a repeated processor that sets an exception response to calls -
 * this is likely to cause transaction rollback or some kind of SOAP fault
 * <p/>
 * If no exception is specified then an empty Exception is thrown
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ExceptionMockDefinitionBuilder extends ContentMockDefinitionBuilderInit<ExceptionMockDefinitionBuilder> {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMockDefinitionBuilder.class);
    private Exception exception;

    /**
     * @param endpointUri A Camel Endpoint URI to listen to for expected messages
     */
    public ExceptionMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

    /**
     * @param exception The exception that will be returned back to the caller
     */
    public ExceptionMockDefinitionBuilder exception(Exception exception) {
        this.exception = exception;
        return self();
    }

    @Override
    public MockDefinition build(MockDefinition previousDefinitionPart) {
        if (exception == null) {
            logger.info("No exception response provided for mock definition endpoint {}, a standard Exception has been used",
                    getEndpointUri());
            exception = new Exception();
        }

        addRepeatedProcessor(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                logger.trace("Setting exception for exchange arriving from endpoint {}", exchange.getFromEndpoint().getEndpointUri());
                exchange.setException(exception);
            }
        });
        return super.build(previousDefinitionPart);
    }
}
