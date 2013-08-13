/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import java.io.InputStream;

/**
 * Locates resources like DTDs required for XML processing.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see XMLReader
 * @since 2013/08
 */
public interface ResourceLocator {

    /**
     * Returns an <tt>InputStream</tt> for the given resource or <code>null</code> if no
     * appropriate file was found.
     *
     * @param name the name of the resource to discover
     * @return an InputStream containing the requested resource or <tt>null</tt> if the resource was not found
     */
    InputStream find(String name);

}
