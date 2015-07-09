package nz.ac.auckland.morc;

import au.com.bytecode.opencsv.CSVReader;
import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;
import nz.ac.auckland.morc.predicate.HttpPathPredicate;
import nz.ac.auckland.morc.processor.MatchedResponseProcessor;
import nz.ac.auckland.morc.processor.SelectorProcessor;
import nz.ac.auckland.morc.resource.*;
import nz.ac.auckland.morc.utility.XmlUtilities;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.SimpleBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.jsonpath.JsonPathExpression;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

public interface MorcMethods {

    XmlUtilities xmlUtilities = new XmlUtilities();

    default XmlUtilities getXmlUtilities() {
        return xmlUtilities;
    }

    default QName soapFaultClient() {
        return qname("http://schemas.xmlsoap.org/soap/envelope/", "Client");
    }

    default QName soapFaultServer() {
        return qname("http://schemas.xmlsoap.org/soap/envelope/", "Server");
    }

    /**
     * @param data An XML string which will be used for seeding a message, or comparing a value
     */
    default XmlTestResource xml(String data) {
        return new XmlTestResource(getXmlUtilities().getXmlAsDocument(data), getXmlUtilities());
    }

    /**
     * @param file A file containing an XML document
     */
    default XmlTestResource xml(File file) {
        return new XmlTestResource(file, getXmlUtilities());
    }

    /**
     * @param url A url pointing to an XML document
     */
    default XmlTestResource xml(URL url) {
        return new XmlTestResource(url, getXmlUtilities());
    }

    default XmlTestResource[] xml(final InputStream... inputs) {
        XmlTestResource[] resources = new XmlTestResource[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            resources[i] = new XmlTestResource(inputs[i], getXmlUtilities());
        }

        return resources;
    }

    /**
     * @param data A JSON string which will be used for seeding a message, or comparing a value
     */
    default JsonTestResource json(String data) {
        return new JsonTestResource(data);
    }

    /**
     * @param file A file containing a JSON document
     */
    default JsonTestResource json(File file) {
        return new JsonTestResource(file);
    }

    /**
     * @param url A url pointing to a JSON document
     */
    default JsonTestResource json(URL url) {
        return new JsonTestResource(url);
    }

    default JsonTestResource[] json(final InputStream... inputs) {
        JsonTestResource[] resources = new JsonTestResource[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            resources[i] = new JsonTestResource(inputs[i]);
        }

        return resources;
    }

    /**
     * @param data A standard Java string which will be used for seeding a message, or comparing a value
     */
    @SuppressWarnings("unchecked")
    default PlainTextTestResource text(String data) {
        return new PlainTextTestResource(data);
    }

    /**
     * @param file A file containing plain text
     */
    @SuppressWarnings("unchecked")
    default PlainTextTestResource text(File file) {
        return new PlainTextTestResource(file);
    }

    /**
     * @param url A url pointing to a plain text document
     */
    @SuppressWarnings("unchecked")
    default PlainTextTestResource text(URL url) {
        return new PlainTextTestResource(url);
    }

    default PlainTextTestResource[] text(final InputStream... inputs) {
        PlainTextTestResource[] resources = new PlainTextTestResource[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            resources[i] = new PlainTextTestResource(inputs[i]);
        }

        return resources;
    }

    default ContentTypeTestResource contentType(String contentType) {
        return new ContentTypeTestResource(contentType);
    }

    /**
     * @param data A map of headers and corresponding data that will be used for seeding a message, or comparing an expected value
     */
    default HeadersTestResource headers(Map<String, Object> data) {
        return new HeadersTestResource(data);
    }

    /**
     * @param file A Java Properties file containing header=value values
     */
    default HeadersTestResource headers(File file) {
        return new HeadersTestResource(file);
    }

    /**
     * @param url A pointer to a Java Properties resource containing header=value values
     */
    default HeadersTestResource headers(URL url) {
        return new HeadersTestResource(url);
    }

    /**
     * @param inputs An InputStream reference to a headers (name=value pairs) resource
     */
    default HeadersTestResource[] headers(final InputStream[] inputs) {
        HeadersTestResource[] resources = new HeadersTestResource[inputs.length];

        for (int i = 0; i < inputs.length; i++) {
            resources[i] = new HeadersTestResource(inputs[i]);
        }

        return resources;
    }

    /**
     * @param header A header to add to an outgoing or expected message
     * @param value  The value that should be assigned to this header
     */
    default HeaderValue header(String header, Object value) {
        return new HeaderValue(header, value);
    }

    /**
     * @param headers A vararg list of headers which can be added to with the header(string,value) function
     */
    default HeadersTestResource headers(HeaderValue... headers) {
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
    default SoapFaultTestResource soapFault(QName faultCode, String message) {
        return new SoapFaultTestResource(faultCode, message);
    }

    /**
     * @param faultCode The fault code to use for the SOAP Fault
     * @param message   The message to use in the SOAP Fault
     * @param xmlDetail The XML detail to use in the SOAP Fault
     */
    default SoapFaultTestResource soapFault(QName faultCode, String message, XmlTestResource xmlDetail) {
        return new SoapFaultTestResource(faultCode, message, xmlDetail);
    }

    /**
     * @return A resource for ensuring an exception occurs, or return back to the client -- will simply use a standard Java Exception instance
     */
    default ExceptionTestResource exception() {
        return new ExceptionTestResource();
    }

    /**
     * @param exception The exception we expect to validate against or return to caller
     * @return A validator for ensuring an exception occurs
     */
    default ExceptionTestResource exception(Exception exception) {
        return new ExceptionTestResource(exception);
    }

    /**
     * @param path A path to a resource on the current classpath
     */
    default URL classpath(String path) {
        URL resource = MorcTest.class.getResource(path);
        if (resource == null) throw new RuntimeException("The classpath resource could not be found: " + path);

        return resource;
    }

    /**
     * @param path A path to a file
     */
    default File file(String path) {
        return new File(path);
    }

    /**
     * A convenience method for specifying matched input to output answers for expectations
     */
    @SuppressWarnings("unchecked")
    default MatchedResponseProcessor matchedResponse(MatchedResponseProcessor.MatchedResponse... responses) {
        return new MatchedResponseProcessor(responses);
    }

    /**
     * A convenience method for specifying matched input validators to an output processor
     */
    @SuppressWarnings("unchecked")
    default MatchedResponseProcessor.MatchedResponse answer(Predicate predicate, Processor processor, Processor... processors) {
        return new MatchedResponseProcessor.MatchedResponse(predicate, processor, processors);
    }

    /**
     * A convenience method for specifying matched input validators to an output processor
     */
    default MatchedResponseProcessor.MatchedResponse response(Predicate predicate, Processor processor, Processor... processors) {
        return answer(predicate, processor, processors);
    }

    /**
     * @param processors The set of processors for the default response if no predicate match can be found
     * @return
     */
    default MatchedResponseProcessor.DefaultMatchedResponse defaultResponse(Processor... processors) {
        return new MatchedResponseProcessor.DefaultMatchedResponse(processors);
    }

    /**
     * A way of paramaterizing resources so that values are updated according to Groovy GStrings
     *
     * @param template   A template of the string resource containing GString variables for substitution
     * @param dataSource A list of name=value pairs that will be used for var substitution. Each entry in the
     *                   list will result in another resource being returned
     */
    default GroovyTemplateTestResource[] groovy(TestResource<String> template, List<Map<String, String>> dataSource) {
        return groovy(template, dataSource, GStringTemplateEngine.class);
    }

    /**
     * A way of paramaterizing resources so that values are updated according to Groovy GStrings
     *
     * @param template   A template of the string resource containing GString variables for substitution
     * @param dataSource A list of name=value pairs that will be used for var substitution. Each entry in the
     *                   list will result in another resource being returned
     */
    @SuppressWarnings("unchecked")
    default GroovyTemplateTestResource[] groovy(String template, List<Map<String, String>> dataSource) {
        return groovy(new PlainTextTestResource(template), dataSource, GStringTemplateEngine.class);
    }

    /**
     * @param template       A template of the string resource containing template-appropriate variables for substitution
     * @param dataSource     A list of name=value pairs that will be used for var substitution. Each entry in the
     *                       list will result in another resource being returned
     * @param templateEngine The template engine, more can be found here: http://groovy.codehaus.org/Groovy+Templates
     */
    default GroovyTemplateTestResource[] groovy(final TestResource<String> template, List<Map<String, String>> dataSource,
                                                Class<? extends TemplateEngine> templateEngine) {
        GroovyTemplateTestResource[] results = new GroovyTemplateTestResource[dataSource.size()];
        try {
            final TemplateEngine engine = templateEngine.newInstance();

            int index = 0;
            for (Map<String, String> variables : dataSource) {
                results[index++] = new GroovyTemplateTestResource(engine, template, variables);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    class VariablePair {
        private String name;
        private String value;

        public VariablePair(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    default VariablePair var(String name, String value) {
        return new VariablePair(name, value);
    }

    /**
     * A way of paramaterizing resources so that values are updated according to Groovy GStrings
     *
     * @param template  A template of the string resource containing GString variables for substitution
     * @param variables A list of name=value pairs that will be used for var substitution. Each entry in the
     *                  list will result in another resource being returned
     */
    default GroovyTemplateTestResource groovy(TestResource<String> template, VariablePair... variables) {
        return groovy(template, GStringTemplateEngine.class, variables);
    }

    /**
     * A way of paramaterizing resources so that values are updated according to Groovy GStrings
     *
     * @param template  A template of the string resource containing GString variables for substitution
     * @param variables A list of name=value pairs that will be used for var substitution. Each entry in the
     *                  list will result in another resource being returned
     */
    @SuppressWarnings("unchecked")
    default GroovyTemplateTestResource groovy(String template, VariablePair... variables) {
        return groovy(new PlainTextTestResource(template), GStringTemplateEngine.class, variables);
    }

    /**
     * @param template       A template of the string resource containing template-appropriate variables for substitution
     * @param variables      A list of name=value pairs that will be used for var substitution. Each entry in the
     *                       list will result in another resource being returned
     * @param templateEngine The template engine, more can be found here: http://groovy.codehaus.org/Groovy+Templates
     */
    default GroovyTemplateTestResource groovy(TestResource<String> template,
                                              Class<? extends TemplateEngine> templateEngine, VariablePair... variables) {
        Map<String, String> map = new HashMap<>();
        for (VariablePair pair : variables) {
            map.put(pair.name, pair.value);
        }

        try {
            TemplateEngine engine = templateEngine.newInstance();
            return new GroovyTemplateTestResource(engine, template, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param templateUrl A URL for a Groovy template that will be evaluated as a plain text document
     * @param variables   A list of name=value pairs that will be used for var substitution. Each entry in the
     *                    list will result in another resource being returned
     */
    @SuppressWarnings("unchecked")
    default GroovyTemplateTestResource groovy(URL templateUrl, VariablePair... variables) {
        return groovy(text(templateUrl), variables);
    }

    /**
     * @param templateUrl    A URL for a Groovy template that will be evaluated as a plain text document
     * @param variables      A list of name=value pairs that will be used for var substitution. Each entry in the
     *                       list will result in another resource being returned
     * @param templateEngine The template engine, more can be found here: http://groovy.codehaus.org/Groovy+Templates
     */
    @SuppressWarnings("unchecked")
    default GroovyTemplateTestResource groovy(URL templateUrl,
                                              Class<? extends TemplateEngine> templateEngine, VariablePair... variables) {
        return groovy(text(templateUrl), templateEngine, variables);
    }

    /**
     * @param groovyResources An array of Groovy templates which result in an XML document
     * @return A list of XmlTestResources that will be evaluated (at runtime) from the Groovy resources
     */
    @SuppressWarnings("unchecked")
    default XmlRuntimeTestResource[] xml(GroovyTemplateTestResource... groovyResources) {
        XmlRuntimeTestResource[] resources = new XmlRuntimeTestResource[groovyResources.length];

        int i = 0;
        for (GroovyTemplateTestResource resource : groovyResources) {
            resources[i++] = new XmlRuntimeTestResource(resource, getXmlUtilities());
        }

        return resources;
    }

    class XmlRuntimeTestResource implements Predicate, Processor {
        private TestResource<String> resource;
        private XmlUtilities xmlUtilities;

        public XmlRuntimeTestResource(TestResource<String> resource, XmlUtilities xmlUtilities) {
            this.resource = resource;
            this.xmlUtilities = xmlUtilities;
        }

        @Override
        public boolean matches(Exchange exchange) {
            return getResource().matches(exchange);
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            getResource().process(exchange);
        }

        @Override
        public String toString() {
            return "XmlGroovyTemplateTestResource:" + getResource().toString();
        }

        private XmlTestResource getResource() {
            try {
                return new XmlTestResource(xmlUtilities.getXmlAsDocument(resource.getValue()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param groovyResources A list of Groovy resources that will be evaluated as JSON documents
     */
    @SuppressWarnings("unchecked")
    default JsonRuntimeTestResource[] json(GroovyTemplateTestResource... groovyResources) {
        JsonRuntimeTestResource[] resources = new JsonRuntimeTestResource[groovyResources.length];

        int i = 0;
        for (GroovyTemplateTestResource resource : groovyResources) {
            resources[i++] = new JsonRuntimeTestResource(resource);
        }

        return resources;
    }

    class JsonRuntimeTestResource implements Processor, Predicate {
        private TestResource<String> resource;

        public JsonRuntimeTestResource(TestResource<String> resource) {
            this.resource = resource;
        }

        @Override
        public boolean matches(Exchange exchange) {
            return getResource().matches(exchange);
        }

        @Override
        public void process(Exchange exchange) throws Exception {
            getResource().process(exchange);
        }

        @Override
        public String toString() {
            return "JsonGroovyTemplateTestResource:" + getResource().toString();
        }

        private JsonTestResource getResource() {
            try {
                return new JsonTestResource(resource.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param groovyResource A list of Groovy resources that will be evaluated as plain text
     */
    default GroovyTemplateTestResource[] text(GroovyTemplateTestResource... groovyResource) {
        return groovyResource;
    }

    /**
     * @param urlpath An Ant-style path to a directory containing test resources for (expected) input and output
     * @return An array of InputStreams that can be used as test resources
     */
    default InputStream[] dir(String urlpath) {
        List<URL> resourceUrls = new ArrayList<>();
        List<InputStream> resourceStreams = new ArrayList<>();

        try {
            Resource[] resources;
            resources = new PathMatchingResourcePatternResolver().getResources(urlpath);
            for (Resource resource : resources) {
                resourceUrls.add(resource.getURL());
            }

            //sort them alphabetically first
            Collections.sort(resourceUrls, (o1, o2) -> o1.toString().compareTo(o2.toString()));

            for (URL url : resourceUrls) {
                resourceStreams.add(url.openStream());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return resourceStreams.toArray(new InputStream[resourceStreams.size()]);
    }

    /**
     * This method can be used as a datasource for the groovy template
     *
     * @param csvResource A reference to a CSV file that contains var values. A header line sets the name of the variables
     * @return A list of variablename-value pairs
     */
    default List<Map<String, String>> csv(TestResource<String> csvResource) {
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    /**
     * @param statusCode the HTTP status code to use or validate
     * @return A resource for non-200 HTTP responses
     */
    default HttpStatusCodeTestResource httpStatusCode(int statusCode) {
        return new HttpStatusCodeTestResource(statusCode);
    }

    /**
     * @param method the HTTP method to use or validate
     * @return A resource for non-GET HTTP responses
     */
    default HttpMethodTestResource httpMethod(HttpMethodTestResource.HttpMethod method) {
        return new HttpMethodTestResource(method);
    }

    default HttpMethodTestResource.HttpMethod POST() {
        return HttpMethodTestResource.HttpMethod.POST;
    }

    default HttpMethodTestResource.HttpMethod GET() {
        return HttpMethodTestResource.HttpMethod.GET;
    }

    default HttpMethodTestResource.HttpMethod DELETE() {
        return HttpMethodTestResource.HttpMethod.DELETE;
    }

    default HttpMethodTestResource.HttpMethod HEAD() {
        return HttpMethodTestResource.HttpMethod.HEAD;
    }

    default HttpMethodTestResource.HttpMethod OPTIONS() {
        return HttpMethodTestResource.HttpMethod.OPTIONS;
    }

    default HttpMethodTestResource.HttpMethod PUT() {
        return HttpMethodTestResource.HttpMethod.PUT;
    }

    default HttpMethodTestResource.HttpMethod TRACE() {
        return HttpMethodTestResource.HttpMethod.TRACE;
    }

    default HttpPathPredicate httpPath(String path) {
        return new HttpPathPredicate(path);
    }

    class HeaderValue {
        private String header;
        private Object value;

        public HeaderValue(String header, Object value) {
            this.header = header;
            this.value = value;
        }
    }

    /**
     * A simple way for generating a QName for SOAP Faults
     */
    default QName qname(String uri, String localName) {
        return new QName(uri, localName);
    }

    class NS {
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
    default NS namespace(String prefix, String uri) {
        return new NS(prefix, uri);
    }

    /**
     * @param time The time in milliseconds to delay the processing of a message (either when publishing to a target or
     *             replying to a system)
     */
    default Processor delay(final long time) {
        return exchange -> Thread.sleep(time);
    }

    /**
     * Returns an XPathBuilder that can be used as a Predicate to evaluate a request or response is as expected
     *
     * @param expression An XPath expression that evaluates the incoming exchange body to a boolean value
     * @param namespaces Namespace definitions used within the XPath expression
     */
    default XPathBuilder xpath(String expression, NS... namespaces) {
        XPathBuilder builder = new XPathBuilder(expression);
        for (NS namespace : namespaces) {
            builder.namespace(namespace.getPrefix(), namespace.getUri());
        }
        return builder;
    }

    /**
     * @param expression A JSONPath Expression that evaluates to true or false
     * @return A predicate that evaluates the JSON Path expression
     */
    default Predicate jsonpath(String expression) {
        return new JsonPathExpression(expression);
    }

    /**
     * @param expression A regular expression to evaluate against the message body
     */
    default Predicate regex(String expression) {
        return new SimpleBuilder("${body} regex '" + expression + "'");
    }

    /**
     * @return randomly choose processors for handling a response
     */
    default Class<? extends SelectorProcessor> randomSelector() {
        return RandomSelector.class;
    }

    class RandomSelector extends SelectorProcessor {
        public RandomSelector(List<Processor> processors) {
            super(processors);
        }

        public void process(Exchange exchange) throws Exception {
            List<Processor> processors = getProcessors();

            if (processors.size() == 0) return;

            processors.get(new Random().nextInt(processors.size())).process(exchange);
        }
    }
}
