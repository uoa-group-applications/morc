package nz.ac.auckland.integration.tests.resource;

import nz.ac.auckland.integration.testing.resource.SoapFaultTestResource;
import nz.ac.auckland.integration.testing.resource.XmlTestResource;
import nz.ac.auckland.integration.testing.utility.XmlUtilities;
import org.apache.cxf.binding.soap.SoapFault;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.namespace.QName;

public class SoapFaultTestResourceTest extends Assert {

    @Before
    public void setUp() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreComments(true);
    }

    @Test
    public void testFaultCodeMessageValid() throws Exception {
        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("foo", "baz"), "foo");

        SoapFault fault = resource.getValue();
        assertEquals(fault.getMessage(), "foo");
        assertEquals(fault.getFaultCode(), new QName("foo", "baz"));
    }

    @Test
    public void testDetailWrapped() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();
        XmlTestResource detail = new XmlTestResource(xmlUtilities.getXmlAsDocument("<detail><foo/></detail>"));

        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("foo", "baz"), "foo", detail);
        SoapFault fault = resource.getValue();
        assertEquals(fault.getMessage(), "foo");
        assertEquals(fault.getFaultCode(), new QName("foo", "baz"));
        assertTrue(detail.validate(fault.getDetail().getOwnerDocument()));
    }

    @Test
    public void testDetailUnwrapped() throws Exception {
        XmlUtilities xmlUtilities = new XmlUtilities();
        XmlTestResource detail = new XmlTestResource(xmlUtilities.getXmlAsDocument("<foo/>"));

        SoapFaultTestResource resource = new SoapFaultTestResource(new QName("foo", "baz"), "foo", detail);
        SoapFault fault = resource.getValue();
        assertEquals(fault.getMessage(), "foo");
        assertEquals(fault.getFaultCode(), new QName("foo", "baz"));
        XmlTestResource detailResource = new XmlTestResource(xmlUtilities.getXmlAsDocument("<detail><foo/></detail>"));
        assertTrue(detailResource.validate(fault.getDetail().getOwnerDocument()));
    }
}
