/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

import sirius.kernel.commons.Context;

class Or extends Constraint {

    protected Constraint[] clauses;

    public static Or on(Constraint... clauses) {
        return new Or(clauses);
    }

    protected Or(Constraint... clauses) {
        this.clauses = clauses;
    }

    @Override
    public boolean addsConstraint() {
        for (Constraint c : clauses) {
            if (c.addsConstraint()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSQL(QueryPathCompiler compiler, String prefix, String parameterId, Context ctx) {
        int subId = 0;
        StringBuilder sb = new StringBuilder();
        for (Constraint c : clauses) {
            if (c.addsConstraint()) {
                if (sb.length() == 0) {
                    sb.append("(");
                } else {
                    sb.append(" ");
                    sb.append(operator());
                    sb.append(" ");
                }
                sb.append(c.getSQL(compiler, prefix, parameterId + "_" + subId, ctx));
            }
            subId++;
        }
        if (sb.length() > 0) {
            sb.append(")");
        }
        return sb.toString();
    }

    protected String operator() {
        return "OR";
    }
}