/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

import sirius.kernel.commons.Context;

/**
 * Implementation for operator constraints: = , &lt; and &gt;. These constraints
 * are accessible via the appropriate factory methods.
 */
class Op extends Constraint {

    public static enum Type {
        LESS("<"), LESS_OR_EQUAL("<="), EQUAL("="), GREATER_OR_EQUAL(">="), GREATER(">"), NOT_EQUAL("<>");

        private final String opCode;

        private Type(String opCode) {
            this.opCode = opCode;
        }

        public String getOpCode() {
            return opCode;
        }
    }

    public static Op eq(String field, Object value) {
        return new Op(Type.EQUAL, value, field, false, false);
    }

    public static Constraint eqIgnoreNull(String field, Object value) {
        return new Op(Type.EQUAL, value, field, true, false);
    }

    public static Constraint eqIngoreCase(String field, Object value) {
        return new Op(Type.EQUAL, value, field, false, true);
    }

    public static Constraint on(String field, Type type, Object value) {
        return new Op(type, value, field, false, false);
    }

    public static Constraint onUnlessNull(String field, Type type, Object value) {
        return new Op(type, value, field, true, false);
    }

    private Type operator;
    private Object object;
    private String field;
    private boolean ignoreNullConstraint;
    private final boolean ignoreCase;

    Op(Type operator, Object object, String field, boolean ignoreNullConstraint, boolean ignoreCase) {
        this.operator = operator;
        this.object = object;
        this.field = field;
        this.ignoreNullConstraint = ignoreNullConstraint;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public boolean addsConstraint() {
        return !ignoreNullConstraint || object != null;
    }

    @Override
    public String getSQL(QueryPathCompiler compiler, String prefix, String parameterId, Context ctx) {
        if (addsConstraint()) {
            PropertyInfo pi = compiler.compile(prefix + "." + field);
            if (ignoreCase) {
                ctx.put(PARAM + parameterId, pi.transform(object == null ? null : object.toString().toLowerCase()));
                return "LOWER(" + pi.getName() + ") " + operator.getOpCode() + " ${" + PARAM + parameterId + "}";
            } else {
                ctx.put(PARAM + parameterId, pi.transform(object));
                return pi.getName() + " " + operator.getOpCode() + " ${" + PARAM + parameterId + "}";
            }
        } else {
            return "";
        }
    }
}