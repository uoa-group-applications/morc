SOAUnit
==================================================
### Orchestrated Testing of SOA Artifacts 

**[Javadoc](http://uoa-group-applications.github.io/soaunit/apidocs/)**

SOAUnit provides a fluent Java Builder/DSL that allows developers and testers to construct specifications that dictate how an integration process/artifact under testing is expected to invoke a variety of different endpoints for a given input, where each endpoint has different ordering and message requirements. Given such a specification, the framework will first set up 'mock' endpoints as specified by the endpoint expectations that provide the canned responses before invoking the integration process/artifact to ensure all requirements are met. The requests/responses are compared semantically to the expectation based on the format required; for example, XML is compared using XMLUnit to allow for variations in XML request/response generation.

This framework was borne out of frustration with setting up automated testing of integration artifacts produced by the mega vendors' integration stacks where we were unable to automatically set up (mock) endpoints to receive messages, ensure they are valid and provide a canned response to let the process continue on its way. This was especially problematic once we started introducing multi-step processes that were invoking other integration artifiacts using a variety of different transports. By using [Apache Camel](http://camel.apache.org/) under the hood we were effectively able to to support a [huge variety of technologies](http://camel.apache.org/components.html) for receiving and sending messages, although our major focus has been on JMS and (SOAP|REST|XML|JSON over HTTP)-style web-services. Our typical work-flow for testing (on a CI box) is to configure our integration stack to point to local host addresses/ports that SOAUnit (and Camel) will use to spin up a subscriber to receive a message, validate and return a canned response. The end result is that we are able to **automatically and repeatedly test Oracle Service Bus (OSB) Artifacts using standard and well-understood tooling.**

As a simple example, we can assume that a PING SOAP-style web-service is running on a mega-vendor's software stack (ESB) that responds with PONG for every PING request. We can create a test specification that sends a request to the service and gets the expected response back:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf:http://localhost:8090/services/pingService","Simple WS PING test")
            .requestBody(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                "<request>PING</request>" +
                             "</ns:pingRequest>"))
            .expectedResponseBody(xml("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                    "<response>PONG</response>" +
                    "</ns:pingResponse>"));
    }
}
```

We can exploit the Camel URI format to use WS-Security username/password credentials by setting the username and
password properties:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf://http://localhost:8090/services/securePingService?wsdlURL=SecurePingService.wsdl&" +
            "properties.ws-security.username=user" +
            "&properties.ws-security.password=pass",
            "Simple WS PING test with WS-Security")
            .requestBody(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                "<request>PING</request>" +
                             "</ns:pingRequest>"))
            .expectedResponseBody(xml("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                    "<response>PONG</response>" +
                    "</ns:pingResponse>"));
    }
}
```
Note that in this case we need to provide a reference to the WSDL because CXF needs to understand the required policy.

Once the requests and responses become larger we will want to put the values into a file, which we can reference from the classpath by changing the xml function parameter:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf:http://localhost:8090/services/pingService","Simple WS PING test with local resources")
            .requestBody(xml(classpath("/data/pingRequest1.xml")))
            .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")));

        //If there's a JSON service we can also ensure this is acting appropriately:
        syncTest("http://localhost:8091/jsonPingService", "Simple JSON PING")
            .requestBody(json("{\"request\":\"PING\"}"))
            .expectedResponseBody(json("{\"response\":\"PONG\"}"));
        //JSON comparisons are made using the Jackson library to unmarshal and compare each value.
    }
}
```


If we change the PING service on the integration stack to pass the request onto another service then we can automatically mock up this service by adding an expectation:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf:http://localhost:8090/services/pingServiceProxy","WS PING test with mock service expectation")
            .requestBody(xml(classpath("/data/pingRequest1.xml")))
            .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
            .addExpectation(syncExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                    .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                    .responseBody(xml(classpath("/data/pingResponse1.xml"))));
    }
}
```
The framework (Camel) will set up a CXF/SOAP endpoint on localhost:9090 which expects the message in `pingRequest1.xml` and will respond with the contents of `pingResponse1.xml`. Note that the advantage with using a Java Builder/Fluent DSL is that code-completion in IDEs can provide hints on what can be added to the specification, in addition to compile-time sanity checks. Furthermore nearly all of the method calls on the builder are optional, meaning it's perfectly acceptable to not set an expectedBody for an expectation if you care only that a request arrives but are not interested in it's content.

The PING service may also test more than one service before providing a response; in this case we need only provide an additional expectation:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf:http://localhost:8090/services/pingServiceMultiProxy","WS PING test with multiple mock service expectations")
            .requestBody(xml(classpath("/data/pingRequest1.xml")))
            .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
            .addExpectation(syncExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                    .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                    .responseBody(xml(classpath("/data/pingResponse1.xml"))))
            .addExpectation(syncExpectation
                    ("cxf:http://localhost:9091/services/anotherTargetWS?wsdlURL=PingService.wsdl")
                    .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                    .responseBody(xml(classpath("/data/pingResponse1.xml"))));
    }
}
```
Note that expectations should occur in the order specified; if each request happens concurrently (e.g. the scatter-gather EIP) then you can relax the ordering requirements:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf:http://localhost:8090/services/pingServiceMultiProxyUnordered","WS PING test with multiple unordered mock service expectations")
            .requestBody(xml(classpath("/data/pingRequest1.xml")))
            .expectedResponseBody(xml(classpath("/data/pingResponse1.xml")))
            .addExpectation(syncExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                    .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                    .responseBody(xml(classpath("/data/pingResponse1.xml")))
                    .ordering(MockExpectation.OrderingType.PARTIAL))
            .addExpectation(syncExpectation("cxf:http://localhost:9091/services/anotherTargetWS?wsdlURL=PingService.wsdl")
                    .expectedBody(xml(classpath("/data/pingRequest1.xml")))
                    .responseBody(xml(classpath("/data/pingResponse1.xml")))
                    .ordering(MockExpectation.OrderingType.PARTIAL));
    }
}
```

We can also test asynchronous services (no response expected) by configuring expectations; for example if we have a message canonicalizer that takes a target-system message off a JMS destination and transforms it to a canonical format for broadcast onto another JMS destination then we can test it by sending a message to the destination and adding an expected message for the output destination:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        asyncTest("vm:test.input", "Simple Asynchronous Canonicalizer Comparison")
            .inputMessage(xml("<SystemField>foo</SystemField>"))
            .addExpectation(asyncExpectation("vm:test.output")
                    .expectedBody(xml("<CanonicalField>foo</CanonicalField>")));
    }
}
```
Note that 'vm' is an in-memory destination queue that is effectively the same as a JMS queue.

Finally, we can also send requests that invoke an exception/fault ensuring that we not only do we receive an exception response but also that the target system never receives the invalid message:
```java
import nz.ac.auckland.integration.testing.OrchestratedTestBuilder;
public class OrchestratedTestSubclassTest extends OrchestratedTestBuilder {
    public static void configure() {
        syncTest("cxf:http://localhost:8090/services/pingServiceProxy","Test invalid message doesn't arrive at the endpoint and returns exception")
            .requestBody(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                                "<request>PONG</request>" +
                                             "</ns:pingRequest>"))
            .expectsExceptionResponse()
            .addExpectation(unreceivedExpectation("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl"));
    }
}
```

All of these examples can be found under at https://github.com/dmacdonald2013/soaunit-example; execute the test run with the standard mvn test goal. The tests themselves are found under com.acme.integration.tests.AcmeTest
