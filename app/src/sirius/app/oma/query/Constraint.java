/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

import sirius.app.oma.Property;
import sirius.kernel.commons.Context;

/**
 * Represents a constraint which can be used for the buildQuery method.
 */
public abstract class Constraint {

    public static final String PARAM = "PARAM";

    /**
     * Used internally to create a SQL conform statement representing the
     * constraint.
     *
     * @param parameterId the index of the parameter, which is used to generate unique
     *                    parameter names.
     */
    public abstract String getSQL(QueryPathCompiler compiler, String prefix, String parameterId, Context parameters);

    /**
     * Determines if this constraint will be used in the query.
     */
    public abstract boolean addsConstraint();

    @Override
    public String toString() {
        return getSQL(QueryPathCompiler.IDENTITY, "entity", "X", Context.create());
    }

}