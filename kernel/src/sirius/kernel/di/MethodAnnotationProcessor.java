/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Implementations of this class will be automatically detected and used to wire objects
 * (to handle annotations placed on methods).
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface MethodAnnotationProcessor {
    /**
     * Returns the trigger-annotation which is used to identify fields of
     * interest.
     *
     * @return the class of the annotation which is used to decide whether a field should be processed by this class.
     */
    @Nonnull
    Class<? extends Annotation> getTrigger();

    /**
     * Invoked for each method which contains the trigger-annotation.
     *
     * @param ctx    the context which can be used to fetch parts from
     * @param method the method to be processed
     * @throws Exception if a part cannot be resolved or the method cannot be filled
     */
    void handle(@Nonnull MutableGlobalContext ctx, @Nonnull Method method) throws Exception;
}
