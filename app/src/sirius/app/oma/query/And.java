/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

class And extends Or {

    public static And on(Constraint... constraints) {
        return new And(constraints);
    }

    public And(Constraint[] clauses) {
        super(clauses);
    }

    @Override
    protected String operator() {
        return "AND";
    }
}