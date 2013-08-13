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
import sirius.kernel.di.PartCollection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Handles the {@link Parts} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.di.AnnotationProcessor
 * @see Parts
 * @since 2013/08
 */
@Register
public class PartsAnnotationProcessor implements AnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Parts.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        Parts parts = field.getAnnotation(Parts.class);
        if (Collection.class.isAssignableFrom(field.getType())) {
            field.set(object, ctx.getParts(parts.value()));
        } else if (PartCollection.class.isAssignableFrom(field.getType())) {
            field.set(object, ctx.getPartCollection(parts.value()));
        } else {
            throw new IllegalArgumentException(
                    "Only fields of type Collection or PartCollection are allowed whe using @Parts.");
        }
    }
}
