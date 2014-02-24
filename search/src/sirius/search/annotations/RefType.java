/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.annotations;

import sirius.search.Cascade;
import sirius.search.Entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as reference to another entity.
 * <p>
 * Fields wearing this annotation must be of type {@link sirius.search.EntityRef}.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RefType {

    /**
     * Contains the type of the referenced entity.
     *
     * @return the type of the referenced entity. Must match the generic parameter of {@link sirius.search.EntityRef}
     */
    Class<? extends Entity> type();

    /**
     * Controls how and if changes and deletes of the referenced entity should be cascaded.
     *
     * @return the cascade style used for the declared relation
     */
    Cascade cascade();

    /**
     * Contains the NLS key used to show an error if the referenced entity was tried to be deleted by a reference still
     * existed.
     *
     * @return the NLS key used to generate an error message when an entity cannot be consistently deleted.
     */
    String onDeleteErrorMsg() default "";

}
