/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import com.scireum.common.Tools;
import com.scireum.ocm.incidents.Incidents;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class XMLGenerator extends XMLStructuredOutput {

    public XMLGenerator() {
        super(new ByteArrayOutputStream());
    }

    public String generate(String encoding) {
        try {
            return new String(((ByteArrayOutputStream) out).toByteArray(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw Incidents.handle(e);
        }
    }

    public String generate() {
        return generate(Tools.UTF_8);
    }
}
