/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.properties;

import org.joda.time.DateMidnight;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;

import java.lang.reflect.Field;

/**
 * Represents a date property which contains no associated time value. This is used to represents fields of type
 * {@link DateMidnight}
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class DateProperty extends Property {

    /**
     * Factory for generating properties based on their field type
     */
    @Register
    public static class Factory implements PropertyFactory {


        @Override
        public boolean accepts(Field field) {
            return DateMidnight.class.equals(field.getType());
        }

        @Override
        public Property create(Field field) {
            return new DateProperty(field);
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
        return DateMidnight.parse(String.valueOf(value));
    }

    /*
     * Instances are only created by the factory
     */
    private DateProperty(Field field) {
        super(field);
    }
}
