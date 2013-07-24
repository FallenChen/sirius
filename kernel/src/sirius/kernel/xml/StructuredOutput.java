/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * Interface for writing structured output like XML or JSON.
 *
 * @author aha
 */
public interface StructuredOutput {

    /**
     * Starts the result
     */
    void beginResult();

    /**
     * Starts the result by specifying the name of the root element.
     */
    void beginResult(String name);

    /**
     * Finishes the result
     */
    void endResult();

    /**
     * Startes a new object with the given name.
     */
    void beginObject(String name);

    /**
     * Startes a new object with the given name and attributes.
     */
    void beginObject(String name, Attribute... attributes);

    /**
     * Ends the currently open object.
     */
    void endObject();

    /**
     * Adds a property to the current object.
     */
    void property(String name, Object data);

    /**
     * Starts an array with is added to the current object as "name".
     */
    void beginArray(String name);

    /**
     * Ends the currently open array.
     */
    void endArray();

}
