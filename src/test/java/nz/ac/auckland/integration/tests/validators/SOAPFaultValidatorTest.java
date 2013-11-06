package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.cxf.binding.soap.SoapFault;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * User: d.macdonald@auckland.ac.nz
 * Date: 23/10/13
 */
@Ignore
public class SOAPFaultValidatorTest extends CamelTestSupport {

    @Test
    public void testThrowException() throws Exception {
        try {
            template.requestBody("cxf:http://localhost:8090/services/pingService?dataFormat=PAYLOAD",
                    new XmlTestResource(getClass().getResource("/data/pingRequestCxf1.xml")).getValue(), String.class);
        } catch (CamelExecutionException e) {
            //new SOAPFaultValidator().validate(e, new XmlTestResource("<foo/>"));
        }

    }

    @Override
    public RouteBuilder createRouteBuilder() throws Exception {

        final SoapFault recoverableFault = new SoapFault("Connection Error", SoapFault.FAULT_CODE_SERVER);
        DocumentBuilderFactory dbf = DocumentBuilderFactory
                .newInstance();
        dbf.setNamespaceAware(true);

        recoverableFault.setDetail(
                dbf.newDocumentBuilder()
                        .parse(this.getClass().getResourceAsStream("/data/soap-fault-detail.xml"))
                        .getDocumentElement());

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("cxf:http://localhost:8090/services/pingService?dataFormat=PAYLOAD&wsdlURL=data/PingService.wsdl")
                        .setFaultBody(constant(recoverableFault));
            }
        };
    }
}
