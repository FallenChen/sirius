package sirius.web.jdbc;

import com.google.common.collect.Maps;
import org.apache.commons.dbcp.BasicDataSource;
import sirius.kernel.commons.Context;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Log;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides a {@link javax.sql.DataSource} which can be configured via the system
 * configuration.
 */
public class Databases {

    protected static final Log LOG = Log.get("db");

    private static final Map<String, Databases> datasources = Maps.newConcurrentMap();
    private final String name;
    private String driver;
    private String url;
    private String username;
    private String password;
    private int initialSize;
    private int maxActive;
    private int maxIdle;
    private boolean testOnBorrow;
    private String validationQuery;

    public static Databases get(String name) {
        Databases ds = datasources.get(name);
        if (ds == null) {
            synchronized (datasources) {
                ds = datasources.get(name);
                if (ds == null) {
                    ds = new Databases(name);
                    datasources.put(name, ds);
                }
            }
        }
        return ds;
    }

    protected Databases(String name) {
        Extension ext = Extensions.getExtension("jdbc.database", name);
        this.name = name;
        this.driver = ext.get("driver").asString();
        this.url = ext.get("url").asString();
        this.username = ext.get("user").asString();
        this.password = ext.get("password").asString();
        this.initialSize = ext.get("initialSize").asInt(0);
        this.maxActive = ext.get("maxActive").asInt(10);
        this.maxIdle = ext.get("maxIdle").asInt(1);
        this.testOnBorrow = ext.get("testOnBorrow").asBoolean();
        this.validationQuery = ext.get("validationQuery").asString();
    }

    private BasicDataSource ds;

    public DataSource getDatasource() {
        if (ds == null) {
            ds = new BasicDataSource();
            initialize();
        }
        return ds;
    }

    public Connection getConnection() throws SQLException {
        return new WrappedConnection(getDatasource().getConnection(), this);
    }

    public JDBCQuery createQuery(String sql) {
        return new JDBCQuery(this, sql);
    }

    public JDBCCall createFunctionCall(String fun, Integer returnType) {
        return new JDBCCall(this, fun, returnType);
    }

    public JDBCCall createProcedureCall(String fun) {
        return new JDBCCall(this, fun, null);
    }

    public void insertRow(String table, Context ctx) throws SQLException {
        Connection c = getConnection();
        try {
            StringBuilder fields = new StringBuilder();
            StringBuilder values = new StringBuilder();
            List<Object> valueList = new ArrayList<Object>();
            for (Map.Entry<String, Object> entry : ctx.entrySet()) {
                if (entry.getValue() != null) {
                    if (fields.length() > 0) {
                        fields.append(", ");
                        values.append(", ");
                    }
                    fields.append(entry.getKey());
                    values.append("?");
                    valueList.add(entry.getValue());
                }
            }
            String sql = "INSERT INTO " + table + " (" + fields.toString() + ") VALUES(" + values + ")";
            PreparedStatement stmt = c.prepareStatement(sql);
            try {
                int index = 1;
                for (Object o : valueList) {
                    try {
                        stmt.setObject(index++, o);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e.getMessage() + " - Index: " + index + ", Value: " + o + ", Query: " + sql,
                                                           e);
                    }
                }
                stmt.executeUpdate();
            } finally {
                stmt.close();
            }
        } finally {
            c.close();
        }
    }

    private void initialize() {
        if (ds != null) {
            ds.setDriverClassName(driver);
            ds.setUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setInitialSize(initialSize);
            ds.setMaxActive(maxActive == 0 ? 20 : maxActive);
            ds.setMaxIdle(maxIdle);
            ds.setTestOnBorrow(testOnBorrow);
            ds.setValidationQuery(validationQuery);
            ds.setMaxWait(1000);
        }
    }

    public String getUrl() {
        return url;
    }

    public int getSize() {
        return maxActive;
    }

    public int getNumIdle() {
        if (ds == null) {
            return 0;
        }
        return ds.getNumIdle();
    }

    public int getNumActive() {
        if (ds == null) {
            return 0;
        }
        return ds.getNumActive();
    }

    public boolean isMySQL() {
        return "com.mysql.jdbc.Driver".equalsIgnoreCase(driver);
    }

}
