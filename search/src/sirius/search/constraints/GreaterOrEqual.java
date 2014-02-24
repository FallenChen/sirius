/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.constraints;

import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Represents a &gt;= constraint for a given field.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class GreaterOrEqual implements Constraint {

    private final String field;
    private final Object value;
    private boolean isFilter;

    /*
     * Use the #on(String, Object) factory method
     */
    private GreaterOrEqual(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    /**
     * Creates the constraint for the given field and value.
     *
     * @param field the field to check
     * @param value the value to compare against
     * @return the newly created constraint for the given field and value
     */
    public static GreaterOrEqual on(String field, Object value) {
        return new GreaterOrEqual(field, value);
    }

    /**
     * Forces this constraint to be applied as filter not as query.
     *
     * @return the constraint itself for fluent method calls
     */
    public GreaterOrEqual asFilter() {
        isFilter = true;
        return this;
    }

    @Override
    public QueryBuilder createQuery() {
        if (!isFilter) {
            return QueryBuilders.rangeQuery(field).gte(value);
        }
        return null;
    }

    @Override
    public FilterBuilder createFilter() {
        if (isFilter) {
            return FilterBuilders.rangeFilter(field).gte(value);
        }
        return null;
    }

    @Override
    public String toString(boolean skipConstraintValues) {
        return field + " > '" + (skipConstraintValues ? "?" : value) + "'";
    }

    @Override
    public String toString() {
        return toString(false);
    }

}
