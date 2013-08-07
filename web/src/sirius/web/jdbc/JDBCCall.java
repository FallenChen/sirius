package sirius.web.jdbc;

import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a flexible way of executing parameterized SQL calls without
 * thinking too much about resource management.
 */
public class JDBCCall {

    private static final String RETURN_VALUE = "_RETVAL";
    private final Databases ds;
    private final List<String> names = new ArrayList<String>();
    private final List<Object> data = new ArrayList<Object>();
    private final List<Integer> types = new ArrayList<Integer>();
    private final String fun;
    private final Map<String, Object> output = new TreeMap<String, Object>();
    private Integer returnType;

    public JDBCCall(Databases ds, String fun, Integer returnType) {
        this.ds = ds;
        this.fun = fun;
        this.returnType = returnType;
        if (returnType != null) {
            addOutParam(RETURN_VALUE, returnType);
        }
    }

    /**
     * Adds an in parameter.
     */
    public JDBCCall addInParam(Object value) {
        names.add("");
        data.add(value);
        types.add(null);
        return this;
    }

    /**
     * Adds an out parameter.
     */
    public JDBCCall addOutParam(String parameter, int type) {
        names.add(parameter);
        data.add(null);
        types.add(type);
        return this;
    }

    /**
     * Invokes the call
     */
    public JDBCCall call() throws SQLException {
        Connection c = ds.getConnection();
        Watch w = Watch.start();
        try {
            StringBuilder sql = new StringBuilder("{call ");
            if (returnType != null) {
                sql.append("? := ");
            }
            sql.append(fun);
            sql.append("(");
            for (int i = returnType != null ? 1 : 0; i < names.size(); i++) {
                if (i > (returnType != null ? 1 : 0)) {
                    sql.append(", ");
                }
                sql.append("?");
            }
            sql.append(")}");

            CallableStatement stmt = c.prepareCall(sql.toString());
            try {
                for (int i = 0; i < names.size(); i++) {
                    if (types.get(i) != null) {
                        stmt.registerOutParameter(i + 1, types.get(i));
                    } else {
                        stmt.setObject(i + 1, data.get(i));
                    }
                }
                stmt.execute();
                for (int i = 0; i < names.size(); i++) {
                    if (types.get(i) != null) {
                        output.put(names.get(i), stmt.getObject(i + 1));
                    }
                }
            } finally {
                stmt.close();
            }
        } finally {
            c.close();
            w.submitMicroTiming("CALL " + fun);
        }

        return this;
    }

    public Value getReturnValue() {
        return getValue(RETURN_VALUE);
    }

    public Value getValue(String key) {
        return Value.of(output.get(key));
    }

    @Override
    public String toString() {
        return "JDBCCall [" + fun + "]";
    }

}
