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
 * Handles the {@link Part} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.di.AnnotationProcessor
 * @see Part
 * @since 1.0
 */
@Register
public class PartAnnotationProcessor implements AnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Part.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        field.set(object, ctx.getPart(field.getType()));
    }
}
