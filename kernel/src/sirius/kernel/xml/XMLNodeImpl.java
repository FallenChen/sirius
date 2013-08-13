/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.google.common.base.Charsets;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sirius.kernel.cache.Cache;
import sirius.kernel.cache.CacheManager;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;

import javax.xml.xpath.*;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link StructuredNode}.
 *
 * @author Andreas Haufler
 * @since 2013/08
 */
class XMLNodeImpl implements StructuredNode {

    /*
     * Cache to improve speed of xpath...
     */
    private static Cache<Tuple<Thread, String>, XPathExpression> cache;

    /*
     * Compiles the given xpath by utilizing the internal cache
     */
    private static XPathExpression compile(String xpath) throws XPathExpressionException {
        Tuple<Thread, String> key = Tuple.create(Thread.currentThread(), xpath);
        if (cache == null) {
            cache = CacheManager.createCache("xpath");
        }
        XPathExpression result = cache.get(key);
        if (result == null) {
            result = XPATH.newXPath().compile(xpath);
            cache.put(key, result);
        }
        return result;
    }

    private static final XPathFactory XPATH = XPathFactory.newInstance();

    private Node node;

    /*
     * Wraps the given node
     */
    protected XMLNodeImpl(Node root) {
        node = root;
    }

    @Override
    public StructuredNode queryNode(String path) throws XPathExpressionException {
        Node result = (Node) compile(path).evaluate(node, XPathConstants.NODE);
        if (result == null) {
            return null;
        }
        return new XMLNodeImpl(result);
    }

    @Override
    public List<StructuredNode> queryNodeList(String path) throws XPathExpressionException {
        NodeList result = (NodeList) compile(path).evaluate(node, XPathConstants.NODESET);
        List<StructuredNode> resultList = new ArrayList<StructuredNode>(result.getLength());
        for (int i = 0; i < result.getLength(); i++) {
            resultList.add(new XMLNodeImpl(result.item(i)));
        }
        return resultList;
    }

    @Override
    public StructuredNode[] queryNodes(String path) throws XPathExpressionException {
        List<StructuredNode> nodes = queryNodeList(path);
        return nodes.toArray(new StructuredNode[nodes.size()]);
    }

    @Override
    public String queryString(String path) throws XPathExpressionException {
        Object result = compile(path).evaluate(node, XPathConstants.NODE);
        if (result == null) {
            return null;
        }
        if (result instanceof Node) {
            String s = ((Node) result).getTextContent();
            if (s != null) {
                return s.trim();
            }
            return s;
        }
        return result.toString().trim();
    }

    @Override
    public String queryXMLString(String path) throws XPathExpressionException {
        XPath xpath = XPATH.newXPath();
        Object result = xpath.evaluate(path, node, XPathConstants.NODE);
        if (result == null) {
            return null;
        }
        if (result instanceof Node) {
            try {
                StringWriter writer = new StringWriter();
                XMLGenerator.writeXML((Node) result, writer, Charsets.UTF_8.name(), true);
                return writer.toString();
            } catch (Throwable e) {
                Exceptions.handle(e);
                return null;
            }
        }
        return result.toString().trim();
    }

    @Override
    public boolean isEmpty(String path) throws XPathExpressionException {
        return Strings.isEmpty(queryString(path));
    }

    @Override
    public String getNodeName() {
        return node.getNodeName();
    }

    @Override
    public String toString() {
        try {
            StringWriter writer = new StringWriter();
            XMLGenerator.writeXML(node.getParentNode(), writer, Charsets.UTF_8.name(), true);
            return writer.toString();
        } catch (Throwable e) {
            Exceptions.handle(e);
            return node.toString();
        }
    }

    @Override
    public Value queryValue(String path) throws XPathExpressionException {
        return Value.of(queryString(path));
    }

}
