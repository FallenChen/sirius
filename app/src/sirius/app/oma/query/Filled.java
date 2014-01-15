/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

/**
 *
 */
package sirius.app.oma.query;

import sirius.kernel.commons.Context;

public class Filled extends Constraint {

    private String field;
    private boolean filled;

    public static Filled on(String field) {
        return new Filled(field, true);
    }

    public static Filled notFilled(String field) {
        return new Filled(field, false);
    }

    private Filled(String field, boolean filled) {
        super();
        this.field = field;
        this.filled = filled;
    }

    @Override
    public boolean addsConstraint() {
        return true;
    }

    @Override
    public String getSQL(QueryPathCompiler compiler, String prefix, String parameterId, Context ctx) {
        if (filled) {
            return compiler.compile(prefix + "." + field) + " IS NOT NULL";
        } else {
            return compiler.compile(prefix + "." + field) + " IS NULL";
        }
    }

}