/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * Used to pass in attributes when creating objects for a StructuredOutput.
 */
public class Attribute {
    private String name;
    private Object value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    private Attribute(String name, Object value) {
        super();
        this.name = name;
        this.value = value;
    }

    public static Attribute set(String name, Object value) {
        return new Attribute(name, value);
    }

}