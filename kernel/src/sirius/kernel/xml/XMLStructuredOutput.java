/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.scireum.common.BusinessException;
import com.scireum.common.Tools;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;

public class XMLStructuredOutput extends AbstractStructuredOutput {

    private TransformerHandler hd;
    protected OutputStream out;
    private int opensCalled = 0;

    public XMLStructuredOutput(OutputStream out) {
        this(out, null);
    }

    public XMLStructuredOutput(OutputStream output, String doctype) {
        try {
            this.out = output;
            StreamResult streamResult = new StreamResult(out);
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            hd = tf.newTransformerHandler();
            Transformer serializer = hd.getTransformer();
            if (doctype != null) {
                serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype);
            }
            serializer.setOutputProperty(OutputKeys.ENCODING, Tools.UTF_8);
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            hd.setResult(streamResult);
            hd.startDocument();
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    @Override
    protected void endArray(String name) {
        try {
            hd.endElement("", "", name);
        } catch (SAXException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    protected void endObject(String name) {
        try {
            hd.endElement("", "", name);
        } catch (SAXException e) {
            throw new BusinessException(e);
        }
    }

    public void endOutput() {
        endObject();
        if (opensCalled-- == 1) {
            super.endResult();
            try {
                hd.endDocument();
                out.flush();
            } catch (SAXException e) {
                throw new BusinessException(e);
            } catch (IOException e) {
                throw new BusinessException(e);
            }
        }
    }

    @Override
    public void endResult() {
        endOutput();
    }

    @Override
    protected void startArray(String name) {
        try {
            hd.startElement("", "", name, null);
        } catch (SAXException e) {
            throw new BusinessException(e);
        }
    }


    @Override
    protected void startObject(String name, Attribute... attributes) {
        try {
            AttributesImpl attrs = null;
            if (attributes != null) {
                attrs = new AttributesImpl();
                for (Attribute attr : attributes) {
                    attrs.addAttribute("", "", attr.getName(), "CDATA", String.valueOf(attr.getValue()));
                }
            }
            hd.startElement("", "", name, attrs);
        } catch (SAXException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    public void beginResult() {
        beginOutput("result"); //$NON-NLS-1$
    }

    @Override
    public void beginResult(String name) {
        beginOutput(name);
    }

    public void beginOutput(String rootElement) {
        if (opensCalled == 0) {
            try {
                hd.startDocument();
            } catch (SAXException e) {
                throw new BusinessException(e);
            }
        }
        opensCalled++;
        beginObject(rootElement);
    }

    public void beginOutput(String rootElement, Attribute... attr) {
        if (opensCalled == 0) {
            try {
                hd.startDocument();
            } catch (SAXException e) {
                throw new BusinessException(e);
            }
        }
        opensCalled++;
        beginObject(rootElement, attr);
    }

    public TagBuilder buildBegin(String rootElement) {
        if (opensCalled == 0) {
            try {
                hd.startDocument();
            } catch (SAXException e) {
                throw new BusinessException(e);
            }
        }
        opensCalled++;
        return buildObject(rootElement);
    }

    @Override
    protected void writeProperty(String name, Object value) {
        try {
            hd.startElement("", "", name, null);
            if (value != null) {
                String val = value.toString();
                hd.characters(val.toCharArray(), 0, val.length());
            }
            hd.endElement("", "", name);
        } catch (SAXException e) {
            throw new BusinessException(e);
        }
    }

    public void text(Object text) {
        try {
            if (text != null) {
                String val = text.toString();
                hd.characters(val.toCharArray(), 0, val.length());
            }
        } catch (SAXException e) {
            throw new BusinessException(e);
        }
    }

    /**
     * Closes the underlying stream
     */
    public void close() throws IOException {
        out.close();
    }

}
