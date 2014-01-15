/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Decides what to do with a relation if an entity is deleted.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OnDelete {
    /**
     * Determines what happens if the relation is not empty and teh owner is
     * deleted.
     */
    DeleteHandler type();

    /**
     * If type is RESTRICT, the given message is used as error, where {0} is
     * replaced with the toString() of the owner.
     */
    String error() default "";
    
}
