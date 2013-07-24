/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractStructuredOutput implements StructuredOutput {

    protected static class Element {
        static final int TYPE_OBJECT = 1;
        static final int TYPE_ARRAY = 2;
        private int type;
        private String name;
        private boolean empty = true;

        public boolean isEmpty() {
            return empty;
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }

        public Element(int type, String name) {
            this.type = type;
            this.name = name;
        }

        public static int getTYPE_ARRAY() {
            return TYPE_ARRAY;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

    }

    public int getCurrentType() {
        if (nesting.isEmpty()) {
            return 0;
        }
        return nesting.get(0).getType();
    }

    public boolean isCurrentObjectEmpty() {
        if (nesting.isEmpty()) {
            return true;
        }
        return nesting.get(0).isEmpty();
    }

    private List<Element> nesting = new ArrayList<Element>();

    @Override
    public void beginArray(String name) {
        startArray(name);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(Element.TYPE_ARRAY, name));

    }

    protected abstract void startArray(String name);

    protected abstract void startObject(String name, Attribute... attributes);

    protected abstract void endArray(String name);

    protected abstract void endObject(String name);

    protected abstract void writeProperty(String name, Object value);

    @Override
    public void beginObject(String name) {
        startObject(name, (Attribute[]) null);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(Element.TYPE_OBJECT, name));
    }

    @Override
    public void beginObject(String name, Attribute... attributes) {
        startObject(name, attributes);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(Element.TYPE_OBJECT, name));
    }

    public class TagBuilder {
        private List<Attribute> attributes = new ArrayList<Attribute>();
        private String name;

        public TagBuilder(String name) {
            this.name = name;
        }

        public TagBuilder addAttribute(String name, String value) {
            attributes.add(Attribute.set(name, value));
            return this;
        }

        public void build() {
            beginObject(name, attributes.toArray(new Attribute[attributes.size()]));
        }
    }

    public TagBuilder buildObject(String name) {
        return new TagBuilder(name);
    }

    @Override
    public void endArray() {
        if (nesting.isEmpty()) {
            throw new IllegalArgumentException("Invalid result structure. No array to close"); //$NON-NLS-1$
        }
        Element e = nesting.get(0);
        nesting.remove(0);
        if (e.getType() != Element.TYPE_ARRAY) {
            throw new IllegalArgumentException("Invalid result structure. No array to close"); //$NON-NLS-1$
        }
        endArray(e.getName());
    }

    @Override
    public void endObject() {
        if (nesting.isEmpty()) {
            throw new IllegalArgumentException("Invalid result structure. No object to close"); //$NON-NLS-1$
        }
        Element e = nesting.get(0);
        nesting.remove(0);
        if (e.getType() != Element.TYPE_OBJECT) {
            throw new IllegalArgumentException("Invalid result structure. No object to close"); //$NON-NLS-1$
        }
        endObject(e.getName());
    }

    @Override
    public void endResult() {
        if (!nesting.isEmpty()) {
            throw new IllegalArgumentException("Invalid result structure. Cannot close result. Objects are still open."); //$NON-NLS-1$
        }
    }

    @Override
    public void property(String name, Object data) {
        if (getCurrentType() != Element.TYPE_OBJECT && getCurrentType() != Element.TYPE_ARRAY) {
            throw new IllegalArgumentException("Invalid result structure. Cannot place a property here."); //$NON-NLS-1$
        }
        writeProperty(name, data);
        nesting.get(0).setEmpty(false);
    }

}
