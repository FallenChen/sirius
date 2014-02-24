/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.properties;

import java.lang.reflect.Field;

/**
 * Represents property which contains a list of other entities. Such a field must wear a
 * {@link sirius.search.annotations.RefType} annotation.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class EntityListProperty extends Property {

    /**
     * Factory for generating properties based on their field type
     */
    public static class Factory implements PropertyFactory {

        @Override
        public boolean accepts(Field field) {
            return String.class.equals(field.getType());
        }

        @Override
        public Property create(Field field) {
            return new EntityListProperty(field);
        }
    }

    /*
     * Instances are only created by the factory
     */
    private EntityListProperty(Field field) {
        super(field);
    }
}
