package sirius.kernel.xml;

import com.scireum.common.Log;
import com.scireum.common.Tools;
import com.scireum.common.data.Value;
import com.scireum.common.rpc.StructuredNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.*;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation for XMLNode
 */
public class XMLNodeImpl implements StructuredNode {

    private static final XPathFactory XPATH = XPathFactory.newInstance();
    private static final Cache<Tuple<Thread, String>, XPathExpression> cache = CacheManager.createCache("XPath",
                                                                                                        100,
                                                                                                        null,
                                                                                                        15,
                                                                                                        TimeUnit.MINUTES,
                                                                                                        null,
                                                                                                        0,
                                                                                                        TimeUnit.SECONDS);

    private static XPathExpression compile(String xpath) throws XPathExpressionException {
        Tuple<Thread, String> key = new Tuple<Thread, String>(Thread.currentThread(), xpath);
        XPathExpression result = cache.get(key);
        if (result == null) {
            result = XPATH.newXPath().compile(xpath);
            cache.put(key, result);
        }
        return result;
    }

    private static final XPathFactory XPATH = XPathFactory.newInstance();
    private Node node;

    /**
     * Overrides the default xpath compiler.
     */
    public static void installCompiler(XPathCompiler compiler) {
        if (compiler != null) {
            xc = compiler;
        }
    }

    public XMLNodeImpl(Node root) {
        node = root;
    }

    @Override
    public StructuredNode queryNode(String path) throws XPathExpressionException {
        Node result = (Node) xc.compile(path).evaluate(node, XPathConstants.NODE);
        if (result == null) {
            return null;
        }
        return new XMLNodeImpl(result);
    }

    @Override
    public List<StructuredNode> queryNodeList(String path) throws XPathExpressionException {
        NodeList result = (NodeList) xc.compile(path).evaluate(node, XPathConstants.NODESET);
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
        Object result = xc.compile(path).evaluate(node, XPathConstants.NODE);
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
                Tools.writeXML((Node) result, writer, Tools.UTF_8, true);
                return writer.toString();
            } catch (Throwable e) {
                Log.UTIL.SEVERE(e);
                return null;
            }
        }
        return result.toString().trim();
    }

    @Override
    public boolean isEmpty(String path) throws XPathExpressionException {
        return Tools.emptyString(queryString(path));
    }

    @Override
    public String getNodeName() {
        return node.getNodeName();
    }

    @Override
    public String toString() {
        try {
            StringWriter writer = new StringWriter();
            Tools.writeXML(node.getParentNode(), writer, Tools.UTF_8, true);
            return writer.toString();
        } catch (Throwable e) {
            Log.UTIL.SEVERE(e);
            return node.toString();
        }
    }

    @Override
    public Value queryValue(String path) throws XPathExpressionException {
        return Value.of(queryString(path));
    }
}
