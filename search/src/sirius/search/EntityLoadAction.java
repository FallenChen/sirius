/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search;

import sirius.kernel.di.ClassLoadAction;
import sirius.kernel.di.MutableGlobalContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

/**
 * Scans for subclasses of {@link Entity} and loads them into the object model so that the {@link Schema} can
 * identify all entity classes.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class EntityLoadAction implements ClassLoadAction {

    @Override
    public Class<? extends Annotation> getTrigger() {
        return null;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Class<?> clazz) throws Exception {
        if (Entity.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
            ctx.registerPart(clazz.newInstance(), Entity.class);
        }
    }
}
