package sirius.web.jdbc;

import com.google.common.io.ByteStreams;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Represents a flexible way of executing parameterized SQL queries without
 * thinking too much about resource management.
 *
 * @see {@link StatementCompiler#buildParameterizedStatement(StatementStrategy, String, Context)}
 *      for the supported language.
 */
public class JDBCQuery {

    public interface RowHandler {
        boolean handle(Row row);
    }

    /**
     * A small wrapper class to represent a result row.
     */
    public static class Row {
        protected Map<String, Object> fields = new TreeMap<String, Object>();

        public Map<String, Object> getFields() {
            return fields;
        }

        public Value getValue(Object key) {
            return Value.of(fields.get(key));
        }

        @SuppressWarnings("unchecked")
        public List<Row> getSublist(Object key) {
            return (List<Row>) getValue(key).get(Collections.emptyList());
        }

        public void setValue(String string, Object value) {
            fields.put(string, value);
        }

        @Override
        public String toString() {
            return "Row [" + fields + "]";
        }

    }

    private final Databases ds;
    private final String sql;
    private Context params = Context.create();

    public JDBCQuery(Databases ds, String sql) {
        this.ds = ds;
        this.sql = sql;
    }

    /**
     * Adds a parameter.
     */
    public JDBCQuery set(String parameter, Object value) {
        params.put(parameter, value);
        return this;
    }

    /**
     * Sets all parameters of the given context.
     */
    public JDBCQuery set(Map<String, Object> ctx) {
        params.putAll(ctx);
        return this;
    }

    /**
     * Executes the given query.
     */
    public List<Row> queryList() throws SQLException {
        return queryList(0);
    }

    /**
     * Executes the given query.
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
     * Executes the given query by invoking the {@link RowHandler} for earch
     * result row.
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
     * Executes the given query.
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
