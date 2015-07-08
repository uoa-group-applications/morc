morc
==================================================
### Mock Orchestrated Testing of SOA and Micro Service Artifacts

**[Javadoc](http://uoa-group-applications.github.io/morc/apidocs/)**

**[Documentation](https://github.com/uoa-group-applications/morc/wiki)**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/nz.ac.auckland.morc/morc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/nz.ac.auckland.morc/morc) [![Build Status](https://travis-ci.org/uoa-group-applications/morc.png)](http://travis-ci.org/uoa-group-applications/morc) [![Coverage Status](https://coveralls.io/repos/uoa-group-applications/morc/badge.png?branch=master)](https://coveralls.io/r/uoa-group-applications/morc?branch=master)

morc provides a fluent Java Builder/DSL that allows developers and testers to construct specifications that dictate how an integration or Micro Service process/artifact under testing is expected to invoke a variety of different endpoints for a given input, where each endpoint has different ordering and message requirements. Given such a specification, the framework will first set up 'mock' endpoints as specified by the endpoint expectations that provide the canned responses before invoking the integration process/artifact to ensure all requirements are met. The requests/responses are compared semantically to the expectation based on the format required; for example, XML is compared using XMLUnit to allow for variations in XML request/response generation.

This framework was borne out of frustration with setting up automated testing of integration artifacts produced by the mega vendors' integration stacks where we were unable to automatically set up (mock) endpoints to receive messages, ensure they are valid and provide a canned response to let the process continue on its way. This was especially problematic once we started introducing multi-step processes that were invoking other integration artifiacts using a variety of different transports. By using [Apache Camel](http://camel.apache.org/) under the hood we were effectively able to to support a [huge variety of technologies](http://camel.apache.org/components.html) for receiving and sending messages, although our major focus has been on JMS and (SOAP|REST|XML|JSON over HTTP)-style web-services. Our typical work-flow for testing (on a CI box) is to configure our integration stack to point to local host addresses/ports that morc (and Camel) will use to spin up a subscriber to receive a message, validate and return a canned response.

As a simple example, we can assume that a PING SOAP-style web-service is running on a mega-vendor's software stack (ESB) that responds with PONG for every PING request. We can create a test specification that sends a request to the service and gets the expected response back:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("Simple WS PING test","cxf:http://localhost:8090/services/pingService")
            .request(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                "<request>PING</request>" +
                             "</ns:pingRequest>"))
            .expectation(xml("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                    "<response>PONG</response>" +
                    "</ns:pingResponse>"));
    }
}
```

While these tests conform with standard JUnit requirements, it requires a lot of boiler plate configuration to get started. A simple way to create and run tests is with a Groovy (>=2.3) script like:
```java
@Grab(group="nz.ac.auckland.morc",module="morc",version="3.3.0")
import nz.ac.auckland.morc.MorcTestBuilder

new MorcTestBuilder() {
    public void configure() {
        syncTest("Simple Echo Test", "http://echo.jsontest.com/foo/baz")
                .expectation(json('{ "foo":"baz" }'))
    }
}.run()
```
Refer to the example project link at the end of the page for further details on getting setup with Maven to manage tests and dependencies.

We can exploit the Camel URI format to use WS-Security username/password credentials by setting the username and
password properties:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("Simple WS PING test with WS-Security","cxf://http://localhost:8090/services/securePingService?" +
            "wsdlURL=SecurePingService.wsdl&" +
            "properties.ws-security.username=user&" +
            "properties.ws-security.password=pass")
            .request(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                "<request>PING</request>" +
                             "</ns:pingRequest>"))
            .expectation(xml("<ns:pingResponse xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                    "<response>PONG</response>" +
                    "</ns:pingResponse>"));
    }
}
```
Note that in this case we need to provide a reference to the WSDL because CXF needs to understand the required policy.

Once the requests and responses become larger we will want to put the values into a file, which we can reference from the classpath by changing the xml function parameter:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("Simple WS PING test with local resources","cxf:http://localhost:8090/services/pingService")
            .request(xml(classpath("/data/pingRequest1.xml")))
            .expectation(xml(classpath("/data/pingResponse1.xml")));

        //If there's a JSON service we can also ensure this is acting appropriately
        //(JSON comparisons are made using the Jackson library to unmarshal and compare each value)
        syncTest("Simple JSON PING","http://localhost:8091/jsonPingService")
            .request(json("{\"request\":\"PING\"}"))
            .expectation(json("{\"response\":\"PONG\"}"));
    }
}
```


If we change the PING service on the integration stack to pass the request onto another service then we can automatically mock up this service by adding an expectation:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("WS PING test with mock service expectation","cxf:http://localhost:8090/services/pingServiceProxy")
            .request(xml(classpath("/data/pingRequest1.xml")))
            .expectation(xml(classpath("/data/pingResponse1.xml")))
            .addMock(syncMock("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                    .expectation(xml(classpath("/data/pingRequest1.xml")))
                    .response(xml(classpath("/data/pingResponse1.xml"))));
    }
}
```
The framework (Camel) will set up a CXF/SOAP endpoint on localhost:9090 which expects the message in `pingRequest1.xml` and will respond with the contents of `pingResponse1.xml`. Note that the advantage with using a Java Builder/Fluent DSL is that code-completion in IDEs can provide hints on what can be added to the specification, in addition to compile-time sanity checks. Furthermore nearly all of the method calls on the builder are optional, meaning it's perfectly acceptable to not set an expectedBody for an expectation if you care only that a request arrives but are not interested in it's content.

The PING service may also test more than one service before providing a response; in this case we need only provide an additional expectation:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("WS PING test with multiple mock service expectations","cxf:http://localhost:8090/services/pingServiceMultiProxy")
            .request(xml(classpath("/data/pingRequest1.xml")))
            .expectation(xml(classpath("/data/pingResponse1.xml")))
            .addMock(syncMock("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                    .expectation(xml(classpath("/data/pingRequest1.xml")))
                    .response(xml(classpath("/data/pingResponse1.xml"))))
            .addMock(syncMock
                    ("cxf:http://localhost:9091/services/anotherTargetWS?wsdlURL=PingService.wsdl")
                    .expectation(xml(classpath("/data/pingRequest1.xml")))
                    .response(xml(classpath("/data/pingResponse1.xml"))));
    }
}
```
Note that expectations should occur in the order specified; if each request happens concurrently (e.g. the scatter-gather EIP) then you can relax the ordering requirements:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("WS PING test with multiple unordered mock service expectations","cxf:http://localhost:8090/services/pingServiceMultiProxyUnordered")
            .request(xml(classpath("/data/pingRequest1.xml")))
            .expectation(xml(classpath("/data/pingResponse1.xml")))
            .addMock(syncMock("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl")
                    .expectation(xml(classpath("/data/pingRequest1.xml")))
                    .response(xml(classpath("/data/pingResponse1.xml")))
                    .ordering(partialOrdering()))
            .addMock(syncMock("cxf:http://localhost:9091/services/anotherTargetWS?wsdlURL=PingService.wsdl")
                    .expectation(xml(classpath("/data/pingRequest1.xml")))
                    .response(xml(classpath("/data/pingResponse1.xml")))
                    .ordering(partialOrdering()));
    }
}
```

We can also test asynchronous services (no response expected) by configuring expectations; for example if we have a message canonicalizer that takes a target-system message off a JMS destination and transforms it to a canonical format for broadcast onto another JMS destination then we can test it by sending a message to the destination and adding an expected message for the output destination:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        asyncTest("Simple Asynchronous Canonicalizer Comparison","vm:test.input")
            .input(xml("<SystemField>foo</SystemField>"))
            .addMock(asyncMock("vm:test.output")
                    .expectation(xml("<CanonicalField>foo</CanonicalField>")));
    }
}
```
Note that 'vm' is an in-memory destination queue that is effectively the same as a JMS queue.

Finally, we can also send requests that invoke an exception/fault ensuring that we not only do we receive an exception response but also that the target system never receives the invalid message:
```java
import nz.ac.auckland.morc.MorcTestBuilder;
public class MorcTest extends MorcTestBuilder {
    @Override
    public void configure() {
        syncTest("Test invalid message doesn't arrive at the endpoint and returns exception","cxf:http://localhost:8090/services/pingServiceProxy")
            .request(xml("<ns:pingRequest xmlns:ns=\"urn:com:acme:integration:wsdl:pingservice\">" +
                                                "<request>PONG</request>" +
                                             "</ns:pingRequest>"))
            .expectsException()
            .addMock(unreceivedMock("cxf:http://localhost:9090/services/targetWS?wsdlURL=PingService.wsdl"));
    }
}
```

All of these examples can be found under at https://github.com/dmacdonald2013/morc-example; execute the test run with the standard mvn test goal. The tests themselves are found under com.acme.integration.tests.AcmeTest
