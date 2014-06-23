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
import sirius.kernel.commons.Value;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;
import sirius.web.security.UserContext;
import sirius.web.http.WebContext;

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
    protected Object transformFromRequest(String name, WebContext ctx) {
        Value value = ctx.get(name);
        if (value.isEmptyString()) {
            return null;
        }
        Object result = NLS.parseUserString(DateMidnight.class, value.getString());
        if (result == null) {
            UserContext.setFieldError(name, value.get());
            throw Exceptions.createHandled()
                            .withNLSKey("Property.invalidInput")
                            .set("field", NLS.get(field.getDeclaringClass().getSimpleName() + "." + name))
                            .set("value", value.asString())
                            .handle();
        }

        return result;
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
