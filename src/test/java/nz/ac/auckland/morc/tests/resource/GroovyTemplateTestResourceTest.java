package nz.ac.auckland.morc.tests.resource;

import groovy.text.SimpleTemplateEngine;
import nz.ac.auckland.morc.resource.GroovyTemplateTestResource;
import nz.ac.auckland.morc.resource.PlainTextTestResource;
import nz.ac.auckland.morc.resource.TestResource;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GroovyTemplateTestResourceTest extends Assert {

    @Test
    public void testGroovySetsValue() throws Exception {
        String templateValue = "$foo $baz $moo";
        PlainTextTestResource resource = new PlainTextTestResource(templateValue);

        Map<String,String> variables = new HashMap<>();
        variables.put("foo","1");
        variables.put("baz","2");
        variables.put("moo","3");

        TestResource testResource = new GroovyTemplateTestResource(resource,variables);
        assertTrue(testResource.toString().endsWith(templateValue));
        assertEquals("1 2 3",testResource.getValue());
    }

    @Test
    public void testGroovyPredicate() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.getIn().setBody("1 2 3");

        String templateValue = "$foo $baz $moo";
        PlainTextTestResource resource = new PlainTextTestResource(templateValue);

        Map<String,String> variables = new HashMap<>();
        variables.put("foo","1");
        variables.put("baz","2");
        variables.put("moo","3");

        GroovyTemplateTestResource testResource = new GroovyTemplateTestResource(resource,variables);
        assertTrue(testResource.matches(e));
        e.getIn().setBody("3 2 1");
        assertFalse(testResource.matches(e));
    }

    @Test
    public void testDifferentTemplateEngine() throws Exception {
        String templateValue = "<% import static org.apache.commons.lang3.text.WordUtils.capitalize %>${capitalize(foo)} ${capitalize(baz)} ${capitalize(moo)}";
        PlainTextTestResource resource = new PlainTextTestResource(templateValue);

        Map<String,String> variables = new HashMap<>();
        variables.put("foo","a");
        variables.put("baz","b");
        variables.put("moo","c");

        GroovyTemplateTestResource testResource = new GroovyTemplateTestResource(new SimpleTemplateEngine(),
                resource,variables);

        assertEquals("A B C",testResource.getValue());
    }

    @Test
    public void testDelayedEvaluation() throws Exception {
        Map<String,String> variables = new HashMap<>();

        String templateValue = "${new Date().getTime()}";
        PlainTextTestResource resource = new PlainTextTestResource(templateValue);

        long startTime = new Date().getTime();
        GroovyTemplateTestResource testResource = new GroovyTemplateTestResource(resource,variables);
        Thread.sleep(2000);
        long endTime = Long.parseLong(testResource.getValue());
        assertTrue(endTime-2000 >= startTime);
        Thread.sleep(2000);
        long nextEndTime = Long.parseLong(testResource.getValue());
        assertTrue(nextEndTime-2000>=endTime);
    }
}
