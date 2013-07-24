package sirius.kernel.xml;

import java.io.InputStream;

/**
 * Locates resources like DTDs required for XML processing.
 *
 * @author aha
 */
public interface ResourceLocator {

    /**
     * Returns an InputStream for the given resource or <code>null</code> if no
     * appropriate file was found.
     */
    InputStream find(String name);

}
