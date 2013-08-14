/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.services;

import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.xml.AbstractStructuredOutput;
import sirius.kernel.xml.Attribute;

import java.io.*;

public class JSONStructuredOutput extends AbstractStructuredOutput {

    private Writer writer;
    private final String callback;

    public JSONStructuredOutput(OutputStream out, String callback, String encoding) {
        try {
            this.callback = callback;
            writer = new OutputStreamWriter(out, encoding);
        } catch (UnsupportedEncodingException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void endArray(String name) {
        try {
            writer.write("]");
        } catch (IOException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void endObject(String name) {
        try {
            writer.write("}");
        } catch (IOException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void startArray(String name) {
        try {
            addRequiredComma();
            if (getCurrentType() == ElementType.OBJECT) {
                writer.write(string(name));
                writer.write(":[");
            } else {
                writer.write("[");
            }
        } catch (IOException e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    protected void startObject(String name, Attribute... attributes) {
        try {
            addRequiredComma();
            if (getCurrentType() == ElementType.OBJECT) {
                writer.write(string(name));
                writer.write(":{");
            } else {
                writer.write("{");
            }
            if (attributes != null) {
                for (Attribute attr : attributes) {
                    property(attr.getName(), attr.getValue());
                }
            }
        } catch (IOException e) {
            throw Exceptions.handle(e);
        }
    }

    private String string(String value) {
        if (value == null || value.length() == 0) {
            return "\"\"";
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
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u" + t.substring(t.length() - 4));
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
            if (Strings.isFilled(callback)) {
                writer.write(callback);
                writer.write("(");
            }
            beginObject("result");
        } catch (IOException e) {
            throw Exceptions.handle(e);
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
            if (getCurrentType() == ElementType.OBJECT) {
                writer.write(string(name));
                writer.write(":");
            }
            if (data == null) {
                writer.write("null");
            } else if (data instanceof Boolean) {
                writer.write(data.toString());
            } else if (data instanceof Number) {
                writer.write(data.toString());
            } else {
                writer.write(string(data.toString()));
            }
        } catch (IOException e) {
            throw Exceptions.handle(e);
        }
    }

    private void addRequiredComma() {
        if (!isCurrentObjectEmpty()) {
            try {
                writer.write(",");
            } catch (IOException e) {
                throw Exceptions.handle(e);
            }
        }

    }

    @Override
    public void endResult() {
        try {
            endObject();
            super.endResult();
            if (Strings.isFilled(callback)) {
                writer.write(")");
            }
            writer.close();
        } catch (IOException e) {
            throw Exceptions.handle(e);
        }
    }

}
