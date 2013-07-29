package sirius.kernel.xml;

import org.w3c.dom.*;
import org.xml.sax.Attributes;
import sirius.kernel.commons.Strings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * Used to create a dom-tree for incoming nodes.
 */
class SAX2DOMHandler {

    private Document document;
    private Node root;
    private Node currentNode;
    private NodeHandler nodeHandler;

    protected SAX2DOMHandler(NodeHandler handler,
                             String uri,
                             String name,
                             Attributes attributes) throws ParserConfigurationException {
        this.nodeHandler = handler;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder loader = factory.newDocumentBuilder();
        document = loader.newDocument();
        createElement(name, attributes);
    }

    private boolean nodeUp() {
        if (isComplete()) {
            nodeHandler.process(new XMLNodeImpl(root));
            return true;
        }
        currentNode = currentNode.getParentNode();
        return false;
    }

    private boolean isComplete() {
        return currentNode.equals(root);
    }

    private void createElement(String name, Attributes attributes) {
        Element element = document.createElement(name);
        for (int i = 0; i < attributes.getLength(); i++) {
            String attrName = attributes.getLocalName(i);
            if (Strings.isEmpty(attrName)) {
                attrName = attributes.getQName(i);
            }
            if (Strings.isFilled(attrName)) {
                element.setAttribute(attrName, attributes.getValue(i));
            }
        }
        if (currentNode != null) {
            currentNode.appendChild(element);
        } else {
            root = element;
            document.appendChild(element);
        }
        currentNode = element;
    }

    public Node getRoot() {
        return root;
    }

    protected void startElement(String uri, String name, Attributes attributes) {
        createElement(name, attributes);
    }

    protected void processingInstruction(String target, String data) {
        ProcessingInstruction instruction = document.createProcessingInstruction(target, data);
        currentNode.appendChild(instruction);
    }

    protected boolean endElement(String uri, String name) {
        if (!currentNode.getNodeName().equals(name)) {
            throw new DOMException(DOMException.SYNTAX_ERR,
                                   "Unexpected end-tag: " + name + " expected: " + currentNode.getNodeName());
        }
        return nodeUp();
    }

    protected void text(String data) {
        currentNode.appendChild(document.createTextNode(data));
    }

    protected NodeHandler getNodeHandler() {
        return nodeHandler;
    }
}
