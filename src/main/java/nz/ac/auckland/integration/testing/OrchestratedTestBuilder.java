package nz.ac.auckland.integration.testing;

import nz.ac.auckland.integration.testing.expectation.*;
import nz.ac.auckland.integration.testing.resource.HeadersTestResource;
import nz.ac.auckland.integration.testing.resource.JsonTestResource;
import nz.ac.auckland.integration.testing.resource.PlainTextTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import nz.ac.auckland.integration.testing.validator.*;

import org.junit.AfterClass;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(value = OrchestratedParameterized.class)
public class OrchestratedTestBuilder extends OrchestratedTest {

    private static List<OrchestratedTestSpecification.AbstractBuilder> specificationBuilders = new ArrayList<>();

    @AfterClass
    public static void clearBuilders() {
        //since this class might be used in more than one place and we're using a static list
        specificationBuilders.clear();
    }

    /**
     * @param endpointUri The endpoint URI that an asynchronous message should be sent to
     * @param description A description for the test specification that clearly identifies it
     * @return An asynchronous test specification builder with the endpoint uri and description configured
     */
    protected static AsyncOrchestratedTestSpecification.Builder asyncTest(String endpointUri, String description) {
        AsyncOrchestratedTestSpecification.Builder builder = new AsyncOrchestratedTestSpecification.Builder(endpointUri, description);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param endpointUri The endpoint URI that a synchronous message should be sent to
     * @param description A description for the test specification that clearly identifies it
     * @return An synchronous test specification builder with the endpoint uri and description configured
     */
    protected static SyncOrchestratedTestSpecification.Builder syncTest(String endpointUri, String description) {
        SyncOrchestratedTestSpecification.Builder builder = new SyncOrchestratedTestSpecification.Builder(endpointUri, description);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param data An XML string which will be used for seeding a message, or comparing a value
     */
    public static XmlTestResource xml(String data) {
        return new XmlTestResource(data);
    }

    /**
     * @param file A file containing an XML document
     */
    public static XmlTestResource xml(File file) {
        return new XmlTestResource(file);
    }

    /**
     * @param url A url pointing to an XML document
     */
    public static XmlTestResource xml(URL url) {
        return new XmlTestResource(url);
    }

    /**
     * @param data          An XML string which will be used for seeding a message, or comparing a value
     * @param xpathSelector An xpath selector for returning certain xml nodes from a response
     */
    public static XmlTestResource xml(String data, XPathSelector xpathSelector) {
        return new XmlTestResource(data, xpathSelector);
    }

    /**
     * @param file          A file containing an XML document
     * @param xpathSelector An xpath selector for returning certain xml nodes from a response
     */
    public static XmlTestResource xml(File file, XPathSelector xpathSelector) {
        return new XmlTestResource(file, xpathSelector);
    }

    /**
     * @param url           A url pointing to an XML document
     * @param xpathSelector An xpath selector for returning certain xml nodes from a response
     */
    public static XmlTestResource xml(URL url, XPathSelector xpathSelector) {
        return new XmlTestResource(url, xpathSelector);
    }

    /**
     * @param data A JSON string which will be used for seeding a message, or comparing a value
     */
    public static JsonTestResource json(String data) {
        return new JsonTestResource(data);
    }

    /**
     * @param file A file containing a JSON document
     */
    public static JsonTestResource json(File file) {
        return new JsonTestResource(file);
    }

    /**
     * @param url A url pointing to a JSON document
     */
    public static JsonTestResource json(URL url) {
        return new JsonTestResource(url);
    }

    /**
     * @param data A standard Java string which will be used for seeding a message, or comparing a value
     */
    public static PlainTextTestResource text(String data) {
        return new PlainTextTestResource(data);
    }

    /**
     * @param file A file containing plain text
     */
    public static PlainTextTestResource text(File file) {
        return new PlainTextTestResource(file);
    }


    /**
     * @param url A url pointing to a plain text document
     */
    public static PlainTextTestResource text(URL url) {
        return new PlainTextTestResource(url);
    }

    /**
     * @param data A map of headers and corresponding data that will be used for seeding a message, or comparing an expected value
     */
    public static HeadersTestResource headers(Map<String, Object> data) {
        return new HeadersTestResource(data);
    }

    /**
     * @param file A Java Properties file containing header=value values
     */
    public static HeadersTestResource headers(File file) {
        return new HeadersTestResource(file);
    }

    /**
     * @param url A pointer to a Java Properties resource containing header=value values
     */
    public static HeadersTestResource headers(URL url) {
        return new HeadersTestResource(url);
    }

    /**
     * Named as such as header interferes with the CamelTestSupport class
     *
     * @param header A header to add to an outgoing or expected message
     * @param value  The value that should be assigned to this header
     */
    public static HeaderValue headervalue(String header, Object value) {
        return header(header, value);
    }

    /**
     * @param header A header to add to an outgoing or expected message
     * @param value  The value that should be assigned to this header
     */
    public static HeaderValue header(String header, Object value) {
        return new HeaderValue(header, value);
    }

    /**
     * @param headers A vararg list of headers which can be added to with the header(string,value) function
     */
    public static HeadersTestResource headers(HeaderValue... headers) {
        Map<String, Object> headersMap = new HashMap<>();
        for (HeaderValue header : headers) {
            headersMap.put(header.header, header.value);
        }

        return headers(headersMap);
    }

    /**
     * @param path A path to a resource on the current classpath
     */
    public static URL classpath(String path) {
        URL resource = OrchestratedTest.class.getResource(path);
        if (resource == null) throw new RuntimeException("The classpath resource could not be found: " + path);

        return resource;
    }

    /**
     * @param path A path to a file
     */
    public static File file(String path) {
        return new File(path);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public static AsyncMockExpectation.Builder asyncExpectation(String endpointUri) {
        return new AsyncMockExpectation.Builder(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public static HttpErrorMockExpectation.Builder wsFaultExpectation(String endpointUri) {
        return new HttpErrorMockExpectation.Builder(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public static ExceptionMockExpectation.Builder exceptionExpectation(String endpointUri) {
        return new ExceptionMockExpectation.Builder(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public static SyncMockExpectation.Builder syncExpectation(String endpointUri) {
        return new SyncMockExpectation.Builder(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that a mock should listen to; should follow the Apache Camel URI format
     */
    public static UnreceivedMockExpectation.Builder unreceivedExpectation(String endpointUri) {
        return new UnreceivedMockExpectation.Builder(endpointUri);
    }

    public static class HeaderValue {
        private String header;
        private Object value;

        public HeaderValue(String header, Object value) {
            this.header = header;
            this.value = value;
        }
    }

    /**
    * @return A validator that ensures that the HTTP responsemeets the expected response body
    */
    public static HttpExceptionValidator http(Validator validator) {
       return new HttpExceptionValidator(validator);
    }

    public static HttpExceptionValidator http(XmlTestResource resource) {
        return http(new XmlValidator(resource));
    }

    public static HttpExceptionValidator http(JsonTestResource resource) {
        return http(new JsonValidator(resource));
    }

    public static HttpExceptionValidator http(PlainTextTestResource resource) {
        return http(new PlainTextValidator(resource));
    }

    public static class NS {
        private String prefix;
        private String uri;

        public NS(String prefix, String uri) {
            this.prefix = prefix;
            this.uri = uri;
        }
    }

    /**
     * @return A namespace designation for xpath evaluation of xml results
     */
    public static NS namespace(String prefix, String uri) {


        return new NS(prefix, uri);
    }

    /**
     * @param xpath      An xpath you want to evaluate to return a node for comparison
     * @param namespaces a collection of namespace pairs used for evaluating the xpath
     * @return an xpath selector to be used for the xml test resource
     */
    public static XPathSelector xpathSelector(String xpath, NS... namespaces) {
        Map<String, String> namespaceMap = new HashMap<>();
        for (NS namespace : namespaces) {
            namespaceMap.put(namespace.prefix, namespace.uri);
        }

        return new XPathSelector(xpath, namespaceMap);
    }

    //this is used by JUnit to initialize each instance of this specification
    public static List<OrchestratedTestSpecification> getSpecifications() {

        List<OrchestratedTestSpecification> specifications = new ArrayList<>();

        for (OrchestratedTestSpecification.AbstractBuilder builder : specificationBuilders) {
            OrchestratedTestSpecification spec = builder.build();
            specifications.add(spec);
        }

        return specifications;
    }
}
