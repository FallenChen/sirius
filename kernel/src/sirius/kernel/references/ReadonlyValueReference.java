/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

/**
 * Baseclass for inner classes which only implements the getValue method.
 */
public abstract class ReadonlyValueReference<E, V> implements ValueReference<E, V> {

    @Override
    public void setValue(E data, V value) {
        throw new UnsupportedOperationException();
    }

}
