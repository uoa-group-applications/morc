package nz.ac.auckland.morc.resource;

import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import java.util.Map;

/**
 * A test resource that has delayed evaluation of a groovy script with appropriate variables
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class GroovyTemplateTestResource implements TestResource<String>, Predicate {
    private TemplateEngine templateEngine;
    private Map<String,String> variables;
    private TestResource<String> template;

    public GroovyTemplateTestResource(TemplateEngine templateEngine, TestResource<String> template, Map<String,String> variables) {
        this.templateEngine = templateEngine;
        this.variables = variables;
        this.template = template;
    }

    public GroovyTemplateTestResource(TestResource<String> template, Map<String,String> variables) {
        this(new GStringTemplateEngine(),template,variables);
    }

    @Override
    public boolean matches(Exchange exchange) {
        PlainTextTestResource textResource;
        try {
            textResource = new PlainTextTestResource(getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return textResource.matches(exchange);
    }

    @Override
    public String getValue() throws Exception {
        Template groovyTemplate = templateEngine.createTemplate(template.getValue());
        return groovyTemplate.make(variables).toString();
    }

    @Override
    public String toString() {
        try {
            String value = "GroovyTemplateTestResource:" + template.getValue();
            if (value.length() < 100) return value;
            else return value.substring(0, 97) + "...";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
