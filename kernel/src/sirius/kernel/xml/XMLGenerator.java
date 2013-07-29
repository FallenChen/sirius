/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.google.common.base.Charsets;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import sirius.kernel.health.Exceptions;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class XMLGenerator extends XMLStructuredOutput {

    public XMLGenerator() {
        super(new ByteArrayOutputStream());
    }

    public String generate(String encoding) {
        try {
            return new String(((ByteArrayOutputStream) out).toByteArray(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw Exceptions.handle(e);
        }
    }

    public String generate() {
        return generate(Charsets.UTF_8.name());
    }

    /**
     * Writes the given XML document to the given writer.
     *
     * @throws javax.xml.transform.TransformerException if an exception during serialization occurs.
     */
    public static void writeXML(Node doc, Writer writer, String encoding) throws TransformerException {
        writeXML(doc, writer, encoding, false);
    }

    public static void writeXML(Node doc,
                                Writer writer,
                                String encoding,
                                boolean omitXMLDeclaration) throws TransformerException {
        StreamResult streamResult = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, encoding);
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        if (omitXMLDeclaration) {
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        doc = doc.getFirstChild();
        while (doc != null) {
            DOMSource domSource = new DOMSource(doc);
            serializer.transform(domSource, streamResult);
            doc = doc.getNextSibling();
        }
    }

    /**
     * Creates a new xml document.
     *
     * @throws javax.xml.parsers.ParserConfigurationException if no suitable xml implementation was found.
     */
    public static Document createDocument(String namespaceURI,
                                          String qualifiedName,
                                          DocumentType docType) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation impl = builder.getDOMImplementation();
        return impl.createDocument(namespaceURI, qualifiedName, docType);
    }
}
