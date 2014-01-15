/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;

/**
 * Implements a LIKE clause as constraint. Implementation is kept private and
 * accessible via the factory methods like and likeIgnoreCase.
 */
public class Like extends Constraint {

    private final String field;
    private final String expression;
    private boolean ignoreCase = false;
    private final boolean wildcardLeft;

    public static Like on(String field, Object value) {
        return new Like(value == null ? null : value.toString(), false, false, field);
    }

    public static Like onIgnoringCase(String field, Object value) {
        return new Like(value == null ? null : value.toString(), true, false, field);
    }

    public static Like in(String field, Object value) {
        return new Like(value == null ? null : value.toString(), false, true, field);
    }

    public static Like inIgnoringCase(String field, Object value) {
        return new Like(value == null ? null : value.toString(), true, true, field);
    }

    private Like(String expression, boolean ignoreCase, boolean wildcardLeft, String field) {
        this.field = field;
        this.expression = expression;
        this.ignoreCase = ignoreCase;
        this.wildcardLeft = wildcardLeft;
    }

    private boolean isUnconstrainted(String prefix) {
        return Strings.isEmpty(prefix) || "%".equals(prefix);
    }

    private String addSQLWildcard(String searchString, boolean wildcardLeft) {
        if (searchString == null) {
            return null;
        }
        if (searchString == "") {
            return "%";
        }
        if ((!searchString.contains("%")) && (searchString.contains("*"))) {
            searchString = searchString.replace('*', '%');
        }
        if (!searchString.endsWith("%")) {
            searchString = searchString + "%";
        }
        if (wildcardLeft && !searchString.startsWith("%")) {
            searchString = "%" + searchString;
        }
        return searchString;
    }

    @Override
    public String getSQL(QueryPathCompiler compiler, String prefix, String parameterId, Context ctx) {
        if (!isUnconstrainted(expression)) {
            PropertyInfo pi = compiler.compile(prefix + "." + field);
            if (ignoreCase) {
                ctx.put(Constraint.PARAM + parameterId,
                        addSQLWildcard((expression == null ? "" : expression).toLowerCase(), wildcardLeft));
                return "LOWER(" + pi.getName() + ") LIKE %{" + PARAM + parameterId + "}";
            } else {
                ctx.put(Constraint.PARAM + parameterId, addSQLWildcard(expression, wildcardLeft));
                return pi.getName() + " LIKE ${" + PARAM + parameterId + "}";
            }
        }
        return "";
    }

    @Override
    public boolean addsConstraint() {
        return !isUnconstrainted(expression);
    }
}