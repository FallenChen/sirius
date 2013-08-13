/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import sirius.kernel.di.AnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Handles the {@link Context} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.di.AnnotationProcessor
 * @see Context
 * @since 2013/08
 */
@Register
public class ContextAnnotationProcessor implements AnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Context.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        field.set(object, ctx);
    }
}
