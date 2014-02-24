/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.constraints;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;

/**
 * Represents a constraint which checks if the given field has not the given value.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class FieldNotEqual implements Constraint {
    private final String field;
    private final Object value;
    private boolean isFilter;

    /*
     * Use the #on(String, Object) factory method
     */
    private FieldNotEqual(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    /**
     * Forces this constraint to be applied as filter not as query.
     *
     * @return the constraint itself for fluent method calls
     */
    public FieldNotEqual asFilter() {
        isFilter = true;
        return this;
    }

    /**
     * Creates a new constraint for the given field and value.
     *
     * @param field the field to check
     * @param value the value to filter for
     * @return a new constraint representing the given filter setting
     */
    public static FieldNotEqual on(String field, Object value) {
        return new FieldNotEqual(field, value);
    }

    @Override
    public QueryBuilder createQuery() {
        if (!isFilter) {
            return Not.on(FieldEqual.on(field, value)).createQuery();
        }
        return null;
    }

    @Override
    public FilterBuilder createFilter() {
        if (isFilter) {
            return Not.on(FieldEqual.on(field, value)).createFilter();
        }
        return null;
    }

    @Override
    public String toString(boolean skipConstraintValues) {
        return field + " != '" + (skipConstraintValues ? "?" : value) + "'";
    }

    @Override
    public String toString() {
        return toString(false);
    }
}
