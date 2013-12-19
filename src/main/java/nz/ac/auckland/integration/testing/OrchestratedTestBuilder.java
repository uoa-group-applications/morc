package nz.ac.auckland.integration.testing;

import au.com.bytecode.opencsv.CSVReader;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import nz.ac.auckland.integration.testing.answer.MatchedInputResponseAnswer;
import nz.ac.auckland.integration.testing.expectation.*;
import nz.ac.auckland.integration.testing.resource.*;
import nz.ac.auckland.integration.testing.specification.AsyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.OrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.specification.SyncOrchestratedTestSpecification;
import nz.ac.auckland.integration.testing.utility.XPathSelector;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import nz.ac.auckland.integration.testing.validator.ExceptionValidator;
import nz.ac.auckland.integration.testing.validator.HttpErrorValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

@RunWith(value = OrchestratedParameterized.class)
public abstract class OrchestratedTestBuilder extends OrchestratedTest {

    private List<OrchestratedTestSpecification.AbstractBuilder> specificationBuilders = new ArrayList<>();
    private static XmlUtilities xmlUtilities = new XmlUtilities();

    public static final QName SOAPFAULT_CLIENT = qname("http://schemas.xmlsoap.org/soap/envelope/", "Client");
    public static final QName SOAPFAULT_SERVER = qname("http://schemas.xmlsoap.org/soap/envelope/", "Server");

    protected abstract void configure();

    /**
     * @param endpointUri The endpoint URI that an asynchronous message should be sent to
     * @param description A description for the test specification that clearly identifies it
     * @return An asynchronous test specification builder with the endpoint uri and description configured
     */
    protected AsyncOrchestratedTestSpecification.Builder asyncTest(String description, String endpointUri) {
        AsyncOrchestratedTestSpecification.Builder builder = new AsyncOrchestratedTestSpecification
                .Builder(description, endpointUri);
        specificationBuilders.add(builder);
        return builder;
    }

    /**
     * @param endpointUri The endpoint URI that a synchronous message should be sent to
     * @param description A description for the test specification that clearly identifies it
     * @return An synchronous test specification builder with the endpoint uri and description configured
     */
    protected SyncOrchestratedTestSpecification.Builder syncTest(String description, String endpointUri) {
        SyncOrchestratedTestSpecification.Builder builder = new SyncOrchestratedTestSpecification
                .Builder(description, endpointUri);
        specificationBuilders.add(builder);
        return builder;
    }

    protected void multiEndpoint(OrchestratedTestSpecification.AbstractBuilder builder,
                                 OrchestratedTestSpecification.AbstractBuilder... builders) {
        specificationBuilders.remove(builder);
        for (OrchestratedTestSpecification.AbstractBuilder b : builders) {
            specificationBuilders.remove(b);
        }
    }

    public static MockExpectation.OrderingType totalOrdering() {
        return MockExpectation.OrderingType.TOTAL;
    }

    public static MockExpectation.OrderingType partialOrdering() {
        return MockExpectation.OrderingType.PARTIAL;
    }

    public static MockExpectation.OrderingType noOrdering() {
        return MockExpectation.OrderingType.NONE;
    }

    /**
     * @param data An XML string which will be used for seeding a message, or comparing a value
     */
    public static XmlTestResource xml(String data) {
        return new XmlTestResource(xmlUtilities.getXmlAsDocument(data));
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

    public static XmlTestResource[] xml(final List<InputStream> inputs) {
        XmlTestResource[] resources = new XmlTestResource[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {
            resources[i] = new XmlTestResource(inputs.get(i));
        }

        return resources;
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

    public static JsonTestResource[] json(final List<InputStream> inputs) {
        JsonTestResource[] resources = new JsonTestResource[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {
            resources[i] = new JsonTestResource(inputs.get(i));
        }

        return resources;
    }

    /**
     * @param data A standard Java string which will be used for seeding a message, or comparing a value
     */
    @SuppressWarnings("unchecked")
    public static PlainTextTestResource text(String data) {
        return new PlainTextTestResource(data);
    }

    /**
     * @param file A file containing plain text
     */
    @SuppressWarnings("unchecked")
    public static PlainTextTestResource text(File file) {
        return new PlainTextTestResource(file);
    }

    /**
     * @param url A url pointing to a plain text document
     */
    @SuppressWarnings("unchecked")
    public static PlainTextTestResource text(URL url) {
        return new PlainTextTestResource(url);
    }

    @SuppressWarnings("unchecked")
    public static PlainTextTestResource[] text(final List<InputStream> inputs) {
        PlainTextTestResource[] resources = new PlainTextTestResource[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {
            resources[i] = new PlainTextTestResource(inputs.get(i));
        }

        return resources;
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

    public static HeadersTestResource[] headers(final List<InputStream> inputs) {
        HeadersTestResource[] resources = new HeadersTestResource[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {
            resources[i] = new HeadersTestResource(inputs.get(i));
        }

        return resources;
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
     * @param faultCode The fault code to use for the SOAP Fault
     * @param message   The message to use in the SOAP Fault
     */
    public static SoapFaultTestResource soapFault(QName faultCode, String message) {
        return new SoapFaultTestResource(faultCode, message);
    }

    /**
     * @param faultCode The fault code to use for the SOAP Fault
     * @param message   The message to use in the SOAP Fault
     * @param xmlDetail The XML detail to use in the SOAP Fault
     */
    public static SoapFaultTestResource soapFault(QName faultCode, String message, XmlTestResource xmlDetail) {
        return new SoapFaultTestResource(faultCode, message, xmlDetail);
    }

    public static ExceptionValidator exception() {
        return new ExceptionValidator();
    }

    public static ExceptionValidator exception(Class<? extends Exception> exception) {
        return new ExceptionValidator(exception);
    }

    public static ExceptionValidator exception(Class<? extends Exception> exception, String message) {
        return new ExceptionValidator(exception, message);
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

    @SuppressWarnings("unchecked")
    public static MatchedInputResponseAnswer matchedResponse(MatchedInputResponseAnswer.MatchedResponse... responses) {
        return new MatchedInputResponseAnswer(responses);
    }

    @SuppressWarnings("unchecked")
    public static MatchedInputResponseAnswer matchedResponse(boolean removeOnMatch,
                                                             MatchedInputResponseAnswer.MatchedResponse... responses) {
        return new MatchedInputResponseAnswer(removeOnMatch, responses);
    }

    @SafeVarargs
    public static MatchedInputResponseAnswer<Map<String, Object>> matchedHeadersResponse(
            MatchedInputResponseAnswer.MatchedResponse<Map<String, Object>>... responses) {
        return new MatchedInputResponseAnswer<>(responses);
    }

    @SafeVarargs
    public static MatchedInputResponseAnswer<Map<String, Object>> matchedHeadersResponse(boolean removeOnMatch,
                                                                                         MatchedInputResponseAnswer.MatchedResponse<Map<String, Object>>... responses) {
        return new MatchedInputResponseAnswer<>(removeOnMatch, responses);
    }

    @SuppressWarnings("unchecked")
    public static MatchedInputResponseAnswer.MatchedResponse answer(Validator validator, TestResource resource) {
        try {
            return new MatchedInputResponseAnswer.MatchedResponse(validator, resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static MatchedInputResponseAnswer.MatchedResponse<Map<String, Object>> headerAnswer(Validator validator,
                                                                                               TestResource<Map<String, Object>> resource) {
        try {
            return new MatchedInputResponseAnswer.MatchedResponse<>(validator, resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<InputStream> groovy(TestResource<String> template, List<Map<String, String>> dataSource) {
        return groovy(template, dataSource, GStringTemplateEngine.class);
    }

    public static List<InputStream> groovy(TestResource<String> template, List<Map<String, String>> dataSource,
                                           Class<? extends TemplateEngine> templateEngine) {
        List<InputStream> results = new ArrayList<>();
        try {
            TemplateEngine engine = templateEngine.newInstance();
            Template groovyTemplate = engine.createTemplate(template.getValue());

            for (Map<String, String> variables : dataSource) {
                results.add(new ReaderInputStream(new StringReader(groovyTemplate.make(variables).toString())));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    public static List<InputStream> dir(String urlpath) {
        List<URL> resourceUrls = new ArrayList<>();
        List<InputStream> resourceStreams = new ArrayList<>();

        try {
            Resource[] resources;
            resources = new PathMatchingResourcePatternResolver().getResources(urlpath);
            for (Resource resource : resources) {
                resourceUrls.add(resource.getURL());
            }

            //sort them alphabetically first
            Collections.sort(resourceUrls, new Comparator<URL>() {
                @Override
                public int compare(URL o1, URL o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });

            for (URL url : resourceUrls) {
                resourceStreams.add(url.openStream());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return resourceStreams;
    }


    public static List<Map<String, String>> csv(TestResource<String> csvResource) {
        CSVReader reader;
        List<Map<String, String>> output = new ArrayList<>();
        String[] headers;

        try {
            reader = new CSVReader(new StringReader(csvResource.getValue()));

            headers = reader.readNext();

            if (new LinkedHashSet<>(Arrays.asList(headers)).size() != headers.length)
                throw new IllegalArgumentException("The headers for the csv " + csvResource + " are not unique");

            String[] nextLine;
            int line = 2;
            while ((nextLine = reader.readNext()) != null) {
                Map<String, String> variableMap = new HashMap<>();

                if (nextLine.length != headers.length)
                    throw new IllegalArgumentException("The CSV resource " + csvResource + " has a different " +
                            "number of headers and values for line " + line);

                for (int i = 0; i < nextLine.length; i++) {
                    variableMap.put(headers[i], nextLine[i]);
                }

                output.add(variableMap);
                line++;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return output;
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
    public static HttpErrorMockExpectation.Builder httpErrorExpectation(String endpointUri) {
        return new HttpErrorMockExpectation.Builder(endpointUri);
    }

    /**
     * @param endpointUri The endpoint URI that the mock should listen to; should follow the Apache Camel URI format
     */
    public static SoapFaultMockExpectation.Builder soapFaultExpectation(String endpointUri) {
        return new SoapFaultMockExpectation.Builder(endpointUri);
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
     * @return A validator that ensures that the HTTP response body meets the expected response
     */
    public static HttpErrorValidator httpExceptionResponse(Validator validator) {
        return new HttpErrorValidator.Builder().responseBodyValidator(validator).build();
    }

    /**
     * @return A validator that ensures that the HTTP response body meets the expected response
     */
    public static HttpErrorValidator httpExceptionResponse(int statusCode, Validator validator) {
        return new HttpErrorValidator.Builder().responseBodyValidator(validator).statusCode(statusCode).build();
    }

    /**
     * @return A validation builder for setting http exception response values
     */
    public static HttpErrorValidator.Builder httpExceptionResponse() {
        return new HttpErrorValidator.Builder();
    }

    /**
     * @param statusCode The HTTP status code that is expected to be received back
     * @return A validator that ensures that the HTTP response meets the expected XML response body
     */
    public static HttpErrorValidator httpExceptionResponse(int statusCode) {
        return new HttpErrorValidator.Builder().statusCode(statusCode).build();
    }

    /**
     * A simple way for generating a QName for SOAP Faults
     */
    public static QName qname(String uri, String localName) {
        return new QName(uri, localName);
    }

    public static class NS {
        private String prefix;
        private String uri;

        public NS(String prefix, String uri) {
            this.prefix = prefix;
            this.uri = uri;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public String getUri() {
            return this.uri;
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
    private List<OrchestratedTestSpecification> getSpecifications() {
        configure();

        List<OrchestratedTestSpecification> specifications = new ArrayList<>();

        for (OrchestratedTestSpecification.AbstractBuilder builder : specificationBuilders) {
            OrchestratedTestSpecification spec = builder.build();
            specifications.add(spec);
        }

        return specifications;
    }
}
