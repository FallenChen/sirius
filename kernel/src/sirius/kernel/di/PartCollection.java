package sirius.kernel.di;

import java.util.Collection;

/**
 * Represents a collection, which always contains all registered parts for the
 * given interface (PartCollection{@link #getInterface()}.
 */
public interface PartCollection<P> extends Iterable<P> {

    /**
     * Returns the interface which are requested by this part collection.
     */
    Class<P> getInterface();

    /**
     * Returns all parts currently registered for the given service interface.
     */
    Collection<P> getParts();
}
