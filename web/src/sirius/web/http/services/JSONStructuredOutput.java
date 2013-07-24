/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http.services;

import com.scireum.common.BusinessException;
import com.scireum.common.Tools;
import sirius.kernel.xml.AbstractStructuredOutput;
import sirius.kernel.xml.Attribute;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class JSONStructuredOutput extends AbstractStructuredOutput {

    private Writer writer;
    private final String callback;

    public JSONStructuredOutput(OutputStream out, String callback, String encoding) throws Exception {
        this.callback = callback;
        writer = new OutputStreamWriter(out, encoding);
    }

    @Override
    protected void endArray(String name) {
        try {
            writer.write("]"); //$NON-NLS-1$
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    protected void endObject(String name) {
        try {
            writer.write("}"); //$NON-NLS-1$
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    protected void startArray(String name) {
        try {
            addRequiredComma();
            if (getCurrentType() == Element.TYPE_OBJECT) {
                writer.write(string(name));
                writer.write(":["); //$NON-NLS-1$
            } else {
                writer.write("["); //$NON-NLS-1$
            }
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    protected void startObject(String name, Attribute... attributes) {
        try {
            addRequiredComma();
            if (getCurrentType() == Element.TYPE_OBJECT) {
                writer.write(string(name));
                writer.write(":{"); //$NON-NLS-1$
            } else {
                writer.write("{"); //$NON-NLS-1$
            }
            if (attributes != null) {
                for (Attribute attr : attributes) {
                    property(attr.getName(), attr.getValue());
                }
            }
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    private String string(String value) {
        if (value == null || value.length() == 0) {
            return "\"\""; //$NON-NLS-1$
        }

        char b;
        char c = 0;
        int i;
        int len = value.length();
        StringBuilder sb = new StringBuilder(len + 4);

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = value.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (b == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b"); //$NON-NLS-1$
                    break;
                case '\t':
                    sb.append("\\t"); //$NON-NLS-1$
                    break;
                case '\n':
                    sb.append("\\n"); //$NON-NLS-1$
                    break;
                case '\f':
                    sb.append("\\f"); //$NON-NLS-1$
                    break;
                case '\r':
                    sb.append("\\r"); //$NON-NLS-1$
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        String t = "000" + Integer.toHexString(c); //$NON-NLS-1$
                        sb.append("\\u" + t.substring(t.length() - 4)); //$NON-NLS-1$
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    @Override
    public void beginResult() {
        try {
            if (Tools.notEmpty(callback)) {
                writer.write(callback);
                writer.write("(");
            }
            beginObject("result"); //$NON-NLS-1$
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    public void beginResult(String name) {
        beginResult();
    }

    @Override
    public void writeProperty(String name, Object data) {
        try {
            addRequiredComma();
            if (getCurrentType() == Element.TYPE_OBJECT) {
                writer.write(string(name));
                writer.write(":"); //$NON-NLS-1$
            }
            if (data == null) {
                writer.write("null"); //$NON-NLS-1$
            } else if (data instanceof Boolean) {
                writer.write(data.toString());
            } else if (data instanceof Number) {
                writer.write(data.toString());
            } else {
                writer.write(string(data.toString()));
            }
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    private void addRequiredComma() {
        if (!isCurrentObjectEmpty()) {
            try {
                writer.write(","); //$NON-NLS-1$
            } catch (IOException e) {
                throw new BusinessException(e);
            }
        }

    }

    @Override
    public void endResult() {
        try {
            endObject();
            super.endResult();
            if (Tools.notEmpty(callback)) {
                writer.write(")");
            }
            writer.flush();
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

}
