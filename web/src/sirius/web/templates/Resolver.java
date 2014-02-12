package sirius.web.templates;

import java.net.URL;

/**
 * Resolves a given resource name to an URL. Implementations can be registered in the component model
 * using {@link sirius.kernel.di.std.Register}.
 * <p>
 * This is used by the {@link Content} to lookup resources.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
public interface Resolver {

    /**
     * Resolves the given resource into an URL.
     *
     * @param resource the resource to resolve
     * @return an URL pointing to the data to use or <tt>null</tt> if this resolver cannot resolve the given resource
     */
    URL resolve(String resource);

}
