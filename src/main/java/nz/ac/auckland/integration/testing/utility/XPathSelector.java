package nz.ac.auckland.integration.testing.utility;

import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.Map;

/**
 * A class for taking documents and apply an xpath to retrieve a single
 * element.
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class XPathSelector {
    String xpathStatement;
    Map<String, String> namespaces;

    /**
     * @param xpathStatement a statement that must return a single element for comparison
     * @param namespaces     a collection of prefix:namespace pairs for evaluating the statement
     */
    public XPathSelector(String xpathStatement, Map<String, String> namespaces) {
        this.namespaces = namespaces;
        this.xpathStatement = xpathStatement;
    }

    /**
     * @param xpathStatement a statement that must return a single element for comparison
     */
    public XPathSelector(String xpathStatement) {
        this.xpathStatement = xpathStatement;
    }

    /**
     * @param input An input document the exception over
     * @return The document after XML evaluation
     * @throws XPathEvaluationException if a Document can not be formed from the xpath
     */
    public synchronized Document evaluate(Document input) throws Exception {

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        if (namespaces != null) {
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.setBindings(namespaces);
            xpath.setNamespaceContext(nsContext);
        }

        XPathExpression xpathExpr = xpath.compile(xpathStatement);
        NodeList result = (NodeList) xpathExpr.evaluate(input, XPathConstants.NODESET);
        if (result.getLength() != 1) throw new XPathEvaluationException("The xpath should return a single element");
        if (!(result.item(0) instanceof Element))
            throw new XPathEvaluationException("The xpath for an XML test resource must return an element");

        Document output = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .newDocument();

        Node resultNode = output.importNode(result.item(0),true);
        output.appendChild(resultNode);

        return output;
    }


    public static class XPathEvaluationException extends Exception {
        public XPathEvaluationException(String reason) {
            super(reason);
        }
    }
}
