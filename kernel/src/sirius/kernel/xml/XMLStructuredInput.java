/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;

public class XMLStructuredInput implements StructuredInput {

    public XMLStructuredInput(InputStream in, boolean close) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(in);
            node = new XMLNodeImpl(doc.getDocumentElement());
            if (close) {
                in.close();
            }
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    private XMLNodeImpl node;

    @Override
    public StructuredNode getNode(String xpath) throws XPathExpressionException {
        return node.queryNode(xpath);
    }

    @Override
    public String toString() {
        return node == null ? "" : node.toString(); //$NON-NLS-1$
    }

    public void setNewParent(StructuredNode node) {
        if (node != null && node instanceof  XMLNodeImpl) {
            this.node = (XMLNodeImpl)node;
        }
    }
}
