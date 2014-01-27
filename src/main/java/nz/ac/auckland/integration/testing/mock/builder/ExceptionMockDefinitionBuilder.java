package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This endpoint will throw an exception of the specified type back to the
 * message consumer; this is likely to cause transaction rollback or some kind
 * of SOAP fault
 * <p/>
 * If no exception is specified then an empty Exception is thrown
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class ExceptionMockDefinitionBuilder extends ContentMockDefinitionBuilder<ExceptionMockDefinitionBuilder> {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMockDefinitionBuilder.class);
    private Exception exception;

    public ExceptionMockDefinitionBuilder(String endpointUri) {
        super(endpointUri);
    }

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
                exchange.setException(exception);
            }
        });
        return super.build(previousDefinitionPart);
    }
}
