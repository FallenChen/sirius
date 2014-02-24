/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.properties;

import org.joda.time.DateTime;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;

import java.lang.reflect.Field;

/**
 * Represents a timestamp property which contains a date along with a time value. This is used to represents fields of type
 * {@link org.joda.time.DateTime}
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class DateTimeProperty extends Property {

    /**
     * Factory for generating properties based on their field type
     */
    @Register
    public static class Factory implements PropertyFactory {

        @Override
        public boolean accepts(Field field) {
            return DateTime.class.equals(field.getType());
        }

        @Override
        public Property create(Field field) {
            return new DateTimeProperty(field);
        }
    }

    @Override
    protected String getMappingType() {
        return "date";
    }

    @Override
    protected Object transformFromSource(Object value) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        return DateTime.parse(String.valueOf(value));
    }

    /*
     * Instances are only created by the factory
     */
    private DateTimeProperty(Field field) {
        super(field);
    }
}
