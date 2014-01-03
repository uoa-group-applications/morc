package nz.ac.auckland.integration.testing.mock.builder;

import nz.ac.auckland.integration.testing.mock.MockExpectation;
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
public class ExceptionMockExpectationBuilder extends ContentMockExpectationBuilder<ExceptionMockExpectationBuilder> {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMockExpectationBuilder.class);
    private Exception exceptionResponse;

    public ExceptionMockExpectationBuilder(String endpointUri) {
         super(endpointUri);
    }

    public ExceptionMockExpectationBuilder exceptionResponse(Exception exception) {
        this.exceptionResponse = exception;
        return self();
    }

    @Override
    public MockExpectation build(MockExpectation previousExpectationPart, int index) {
        if (exceptionResponse == null) {
            logger.info("No exception response provided for endpoint {}, a standard Exception has been used",
                    getEndpointUri());
            exceptionResponse = new Exception();
        }

        addRepeatedProcessor(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setException(exceptionResponse);
            }
        });
        return super.build(previousExpectationPart,index);
    }
}
