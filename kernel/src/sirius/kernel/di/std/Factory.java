/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inserts a factory registered in the <tt>GlobalContext</tt>.
 * <p>
 * The target field must have {@link java.util.function.Supplier} as type. A factory can be created by placing a
 * {@link sirius.kernel.di.std.Register} annotation on a static public method. This can also be accessed via
 * {@link sirius.kernel.di.GlobalContext#make(Class, String)} or
 * {@link sirius.kernel.di.GlobalContext#getFactory(Class, String)}. Using this annotation instead of one of the
 * methods above yields in better performance for very frequent calls, as only a single lookup is performed.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/05
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Factory {

    /**
     * Determines the type of objects produced by the factory
     *
     * @return the type of objects produced by the factory
     */
    Class<?> type();

    /**
     * The name of the requested factory
     *
     * @return the name which identifies the requested factory
     */
    String name();

}
