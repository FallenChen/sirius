/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.jdbc;

import com.google.common.io.ByteStreams;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Watch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a flexible way of executing parameterized SQL queries without
 * thinking too much about resource management.
 * <p>
 * Supports named parameters in form of ${name}. Also #{name} can be used in LIKE expressions and will be
 * surrounded by % signs (if not empty).
 * </p>
 * <p>
 * Optional blocks can be surrounded with angular braces: SELECT * FROM x WHERE test = 1[ AND test2=${val}]
 * The surrounded block will only be added to the query, if the parameter within has a non-null value.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public class JDBCQuery {

    public interface RowHandler {
        boolean handle(Row row);
    }

    private final Databases ds;
    private final String sql;
    private Context params = Context.create();

    /*
     * Create a new instance using Databases.createQuery(sql)
     */
    protected JDBCQuery(Databases ds, String sql) {
        this.ds = ds;
        this.sql = sql;
    }

    /**
     * Adds a parameter.
     *
     * @param parameter the name of the parameter as referenced in the SQL statement (${name} or #{name}).
     * @param value     the value of the parameter
     * @return the query itself to support fluent calls
     */
    public JDBCQuery set(String parameter, Object value) {
        params.put(parameter, value);
        return this;
    }

    /**
     * Sets all parameters of the given context.
     *
     * @param ctx the containing pairs of names and values to add to the query
     * @return the query itself to support fluent calls
     */
    public JDBCQuery set(Map<String, Object> ctx) {
        params.putAll(ctx);
        return this;
    }

    /**
     * Executes the given query returning the result as list
     *
     * @return a list of {@link Row}s
     * @throws SQLException in case of a database error
     */
    public List<Row> queryList() throws SQLException {
        return queryList(0);
    }

    /**
     * Executes the given query returning the result as list with at most <tt>maxRows</tt> entries
     *
     * @param maxRows maximal number of rows to be returned
     * @return a list of {@link Row}s
     * @throws SQLException in case of a database error
     */
    public List<Row> queryList(int maxRows) throws SQLException {
        Watch w = Watch.start();
        Connection c = ds.getConnection();
        List<Row> result = new ArrayList<Row>();
        String finalSQL = sql;
        try {
            SQLStatementStrategy sa = new SQLStatementStrategy(c, ds.isMySQL());
            StatementCompiler.buildParameterizedStatement(sa, sql, params);
            if (sa.getStmt() == null) {
                return result;
            }
            finalSQL = sa.getQueryString();
            if (maxRows > 0) {
                sa.getStmt().setMaxRows(maxRows);
            }
            ResultSet rs = sa.getStmt().executeQuery();
            try {
                while (rs.next()) {
                    Row row = new Row();
                    for (int col = 1; col <= rs.getMetaData().getColumnCount(); col++) {
                        row.fields.put(rs.getMetaData().getColumnLabel(col), rs.getObject(col));
                    }
                    result.add(row);
                }
                return result;
            } finally {
                rs.close();
                sa.getStmt().close();
            }

        } finally {
            c.close();
            w.submitMicroTiming(finalSQL);
        }
    }

    /**
     * Executes the given query by invoking the {@link RowHandler} for each
     * result row.
     *
     * @param handler the row handler invoked for each row
     * @throws SQLException in case of a database error
     */
    public void perform(RowHandler handler) throws SQLException {
        Watch w = Watch.start();
        Connection c = ds.getConnection();
        String finalSQL = sql;
        try {
            SQLStatementStrategy sa = new SQLStatementStrategy(c, ds.isMySQL());
            StatementCompiler.buildParameterizedStatement(sa, sql, params);
            if (sa.getStmt() == null) {
                return;
            }
            finalSQL = sa.getQueryString();
            ResultSet rs = sa.getStmt().executeQuery();
            try {
                while (rs.next()) {
                    Row row = new Row();
                    for (int col = 1; col <= rs.getMetaData().getColumnCount(); col++) {
                        row.fields.put(rs.getMetaData().getColumnLabel(col), rs.getObject(col));
                    }
                    if (!handler.handle(row)) {
                        break;
                    }
                }
            } finally {
                rs.close();
                sa.getStmt().close();
            }

        } finally {
            c.close();
            w.submitMicroTiming(finalSQL);
        }
    }

    /**
     * Executes the given query returning the first matching row.
     *
     * @return the first matching row for the given query or <tt>null</tt> if no matching row was found
     * @throws SQLException in case of a database error
     */
    public Row queryFirst() throws SQLException {
        Connection c = ds.getConnection();
        Watch w = Watch.start();
        String finalSQL = sql;
        try {
            SQLStatementStrategy sa = new SQLStatementStrategy(c, ds.isMySQL());
            StatementCompiler.buildParameterizedStatement(sa, sql, params);
            if (sa.getStmt() == null) {
                return null;
            }
            ResultSet rs = sa.getStmt().executeQuery();
            try {
                if (rs.next()) {
                    Row row = new Row();
                    for (int col = 1; col <= rs.getMetaData().getColumnCount(); col++) {
                        Object obj = rs.getObject(col);
                        if (obj instanceof Blob) {
                            OutputStream out = (OutputStream) params.get(rs.getMetaData().getColumnLabel(col));
                            if (out != null) {
                                try {
                                    Blob blob = (Blob) obj;
                                    InputStream in = blob.getBinaryStream();
                                    try {
                                        ByteStreams.copy(in, out);
                                    } finally {
                                        in.close();
                                    }
                                } catch (IOException e) {
                                    throw new SQLException(e);
                                }
                            }
                        } else {
                            row.fields.put(rs.getMetaData().getColumnLabel(col), obj);
                        }
                    }
                    return row;
                }
                return null;
            } finally {
                rs.close();
                sa.getStmt().close();
            }

        } finally {
            c.close();
            w.submitMicroTiming(finalSQL);
        }
    }

    /**
     * Executes the query as update.
     * <p>
     * Requires the SQL statement to be an UPDATE or DELETE statement.
     * </p>
     *
     * @return the number of rows changed
     * @throws SQLException in case of a database error
     */
    public int executeUpdate() throws SQLException {
        Connection c = ds.getConnection();
        Watch w = Watch.start();
        String finalSQL = sql;

        try {
            SQLStatementStrategy sa = new SQLStatementStrategy(c, ds.isMySQL());
            StatementCompiler.buildParameterizedStatement(sa, sql, params);
            if (sa.getStmt() == null) {
                return 0;
            }
            try {
                return sa.getStmt().executeUpdate();
            } finally {
                sa.getStmt().close();
            }
        } finally {
            c.close();
            w.submitMicroTiming(finalSQL);
        }
    }

    /**
     * Executes the update and returns the generated keys.
     * <p>
     * Requires the SQL statement to be an UPDATE or DELETE statement.
     * </p>
     *
     * @return the a row representing all generated keys
     * @throws SQLException in case of a database error
     */
    public Row executeUpdateReturnKeys() throws SQLException {
        Connection c = ds.getConnection();
        Watch w = Watch.start();
        String finalSQL = sql;

        try {
            SQLStatementStrategy sa = new SQLStatementStrategy(c, ds.isMySQL());
            sa.setRetriveGeneratedKeys(true);
            StatementCompiler.buildParameterizedStatement(sa, sql, params);
            if (sa.getStmt() == null) {
                return new Row();
            }
            try {
                sa.getStmt().executeUpdate();
                ResultSet rs = sa.getStmt().getGeneratedKeys();
                try {
                    Row row = new Row();
                    if (rs.next()) {
                        for (int col = 1; col <= rs.getMetaData().getColumnCount(); col++) {
                            row.fields.put(rs.getMetaData().getColumnLabel(col), rs.getObject(col));
                        }
                    }
                    return row;
                } finally {
                    rs.close();
                }
            } finally {
                sa.getStmt().close();
            }
        } finally {
            c.close();
            w.submitMicroTiming(finalSQL);
        }
    }

    @Override
    public String toString() {
        return "JDBCQuery [" + sql + "]";
    }

}
