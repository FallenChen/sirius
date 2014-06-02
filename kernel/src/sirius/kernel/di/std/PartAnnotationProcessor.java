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
 * @since 2013/08
 */
@Register
public class PartAnnotationProcessor implements AnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Part.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        Object part = ctx.getPart(field.getType());
        if (part != null) {
            field.set(object, part);
        }
    }
}
