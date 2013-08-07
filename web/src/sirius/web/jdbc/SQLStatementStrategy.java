package sirius.web.jdbc;

import sirius.kernel.commons.Tuple;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for filling a SQL query in the {@link QueryValve}.
 */
public class SQLStatementStrategy implements StatementStrategy {

    private PreparedStatement stmt;
    private List<Tuple<Integer, Object>> parameters = new ArrayList<Tuple<Integer, Object>>();
    private Connection c;
    private StringBuilder sb;
    private boolean mysql;
    private boolean retriveGeneratedKeys;

    public SQLStatementStrategy(Connection c, boolean mysql) {
        this.c = c;
        this.mysql = mysql;
        this.sb = new StringBuilder();
    }

    @Override
    public void appendString(String queryPart) {
        sb.append(queryPart);
    }

    @Override
    public void set(int idx, Object value) throws SQLException {
        parameters.add(new Tuple<Integer, Object>(idx, value));
    }

    @Override
    public String getParameterName() {
        return "?";
    }

    @Override
    public String getQueryString() {
        return sb.toString();
    }

    public PreparedStatement getStmt() throws SQLException {
        if (stmt == null) {
            if (retriveGeneratedKeys) {
                stmt = c.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
            } else {
                stmt = c.prepareStatement(sb.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            }
            if (mysql) {
                // Switch MySQL driver into streaming mode...
                stmt.setFetchSize(Integer.MIN_VALUE);
            }
            for (Tuple<Integer, Object> t : parameters) {
                stmt.setObject(t.getFirst(), t.getSecond());
            }

        }
        return stmt;
    }

    public boolean isRetriveGeneratedKeys() {
        return retriveGeneratedKeys;
    }

    public void setRetriveGeneratedKeys(boolean retriveGeneratedKeys) {
        this.retriveGeneratedKeys = retriveGeneratedKeys;
    }
}
