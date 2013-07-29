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

    protected static enum ElementType {
        UNKNOWN, OBJECT, ARRAY;
    }

    protected static class Element {

        private ElementType type;
        private String name;
        private boolean empty = true;

        protected boolean isEmpty() {
            return empty;
        }

        protected void setEmpty(boolean empty) {
            this.empty = empty;
        }

        protected Element(ElementType type, String name) {
            this.type = type;
            this.name = name;
        }

        protected ElementType getType() {
            return type;
        }

        protected String getName() {
            return name;
        }

    }

    public ElementType getCurrentType() {
        if (nesting.isEmpty()) {
            return ElementType.UNKNOWN;
        }
        return nesting.get(0).getType();
    }

    public boolean isCurrentObjectEmpty() {
        if (nesting.isEmpty()) {
            return true;
        }
        return nesting.get(0).isEmpty();
    }

    protected List<Element> nesting = new ArrayList<Element>();

    @Override
    public void beginArray(String name) {
        startArray(name);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(ElementType.ARRAY, name));
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
        nesting.add(0, new Element(ElementType.OBJECT, name));
    }

    @Override
    public void beginObject(String name, Attribute... attributes) {
        startObject(name, attributes);
        if (!nesting.isEmpty()) {
            nesting.get(0).setEmpty(false);
        }
        nesting.add(0, new Element(ElementType.OBJECT, name));
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
        if (e.getType() != ElementType.ARRAY) {
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
        if (e.getType() != ElementType.OBJECT) {
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
        if (getCurrentType() != ElementType.OBJECT && getCurrentType() != ElementType.ARRAY) {
            throw new IllegalArgumentException("Invalid result structure. Cannot place a property here."); //$NON-NLS-1$
        }
        writeProperty(name, data);
        nesting.get(0).setEmpty(false);
    }

}
