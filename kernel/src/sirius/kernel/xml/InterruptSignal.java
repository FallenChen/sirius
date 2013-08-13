/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

/**
 * This signal can be used to interrupt XML processing.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see XMLReader
 * @since 2013/08
 */
public interface InterruptSignal {
    /**
     * Returns <tt>true</tt> if xml processing should be interrupted or <tt>false</tt> if the parser should continue
     *
     * @return whether xml parsing should be interrupted (<tt>true</tt>) or not (<tt>false</tt>).
     */
    boolean isInterrupted();
}
