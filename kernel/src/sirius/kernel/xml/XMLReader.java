/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * A combination of DOM and SAX parser which permits to parse very large XML files while conveniently handling sub tree
 * using a DOM and xpath api.
 * <p>
 * Used SAX to parse a given XML file. A set of {@link NodeHandler} objects can be given, which get notified if
 * a sub-tree below a given tag was parsed. This sub-tree is available as DOM and can conveniently be processed
 * using xpath.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class XMLReader extends DefaultHandler {

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        // Delegate to active handlers...
        String cData = new String(ch).substring(start, start + length);
        for (SAX2DOMHandler handler : activeHandlers) {
            handler.text(cData);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        // Consider iterating over all activeHandler which are not complete
        // yet and raise an exception.
        // For now this is simply ignored to make processing more robust.
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        // Delegate to active handlers and deletes them if they are finished...
        Iterator<SAX2DOMHandler> iter = activeHandlers.iterator();
        while (iter.hasNext()) {
            SAX2DOMHandler handler = iter.next();
            if (handler.endElement(uri, name)) {
                iter.remove();
            }
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        // Delegate to active handlers...
        for (SAX2DOMHandler handler : activeHandlers) {
            handler.processingInstruction(target, data);
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        // Delegate to active handlers...
        for (SAX2DOMHandler handler : activeHandlers) {
            handler.startElement(uri, name, attributes);
        }
        // Start a new handler is necessary
        try {
            // QName qualifiedName = new QName(uri, localName);
            NodeHandler handler = handlers.get(name);
            if (handler != null) {
                activeHandlers.add(new SAX2DOMHandler(handler, uri, name, attributes));
            }
        } catch (ParserConfigurationException e) {
            throw new SAXException(e);
        }
        /**
         * Check if the user tried to interrupt parsing....
         */
        if (interrupt != null && interrupt.isInterrupted()) {
            throw new UserInterruptException();
        }
    }

    private Map<String, NodeHandler> handlers = new TreeMap<String, NodeHandler>();
    private List<SAX2DOMHandler> activeHandlers = new ArrayList<SAX2DOMHandler>();
    private InterruptSignal interrupt;

    /**
     * Registers a new handler for a qualified name of a node.
     * <p>
     * Handlers are invoked after the complete node was read. Namespaces are ignored for now which eases
     * the processing a lot (especially for xpath related tasks). Namespaces however
     * could be easily added by replacing String with QName here.
     * </p>
     *
     * @param name    the qualified name of the tag which should be parsed and processed
     * @param handler the NodeHandler used to process the parsed DOM sub-tree
     */
    public void addHandler(String name, NodeHandler handler) {
        handlers.put(name, handler);
    }

    /**
     * Parses the given stream.
     *
     * @throws IOException if parsing the XML fails either due to an IO error or due to an SAXException (when
     *                     processing a malformed XML).
     */
    public void parse(InputStream stream) throws IOException {
        parse(stream, null, null);
    }

    /**
     * Returns a {@link StructuredNode} for the given w3c DOM node.
     *
     * @param node the DOM node to wrap
     * @return a <tt>StructuredNode</tt> wrapping the given node
     */
    public static StructuredNode convert(Node node) {
        return new XMLNodeImpl(node);
    }

    /*
     * Used to handle the InterruptSignal
     */
    class UserInterruptException extends RuntimeException {

        private static final long serialVersionUID = -7454219131982518216L;
    }

    /**
     * Parses the given stream using the given locator and interrupt signal.
     *
     * @param stream    the stream containing the XML data
     * @param locator   the resource locator used to discover dependent resources
     * @param interrupt an InterruptSignal which can be used to cancel XML parsing
     * @throws IOException if parsing the XML fails either due to an IO error or due to an SAXException (when
     *                     processing a malformed XML).
     */
    public void parse(InputStream stream, final ResourceLocator locator, InterruptSignal interrupt) throws IOException {
        this.interrupt = interrupt;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            org.xml.sax.XMLReader reader = saxParser.getXMLReader();
            reader.setEntityResolver(new EntityResolver() {

                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    URL url = new URL(systemId);
                    // Check if file is local
                    if ("file".equals(url.getProtocol())) {
                        // Check if file exists
                        File file = new File(url.getFile());
                        if (file.exists()) {
                            return new InputSource(new FileInputStream(file));
                        }
                        // File not existent -> try to resolve via
                        // classloaders...
                        if (locator != null) {
                            String name = file.getName();
                            InputStream stream = locator.find(name);
                            if (stream != null) {
                                return new InputSource(stream);
                            }
                        }
                    }
                    // We pretend that we found an empty DTD....
                    return new InputSource(new StringReader(""));
                }
            });
            reader.setContentHandler(this);
            reader.parse(new InputSource(stream));
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (UserInterruptException e) {
            /*
             * IGNORED - this is used to cancel parsing if the used tried to
			 * cancel a process.
			 */
        } finally {
            stream.close();
        }
    }
}
