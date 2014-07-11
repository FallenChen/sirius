/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.jdbc;

import com.google.common.collect.Maps;
import org.apache.commons.dbcp.BasicDataSource;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Average;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Log;
import sirius.kernel.nls.Formatter;
import sirius.web.health.MetricProvider;
import sirius.web.health.MetricsCollector;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides a {@link javax.sql.DataSource} which can be configured via the system
 * configuration.
 * <p>
 * Use <code>Database.get("name")</code> to obtain a managed connection to the database. For most queries,
 * prefer {@link #createQuery(String)} to create and execute a SQL query as it has connection management built in.
 * </p>
 * <p>
 * Configuration is done via the system configuration. To declare a database provide an extension in
 * "jdbc.database". For examples see "component-web.conf".
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public class Database {

    protected static final Log LOG = Log.get("db");
    private static final Map<String, Database> datasources = Maps.newConcurrentMap();

    protected static Counter numUses = new Counter();
    protected static Counter numQueries = new Counter();
    protected static Average queryDuration = new Average();

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
    private BasicDataSource ds;

    /**
     * Provides some metrics across all managed data sources.
     */
    @Register
    public static class DatabaseMetricProvider implements MetricProvider {

        @Override
        public void gather(MetricsCollector collector) {
            // Only report statistics if we have at least one database connection...
            if (!datasources.isEmpty()) {
                collector.differentialMetric("jdbc-use", "db-uses", "JDBC Uses", numUses.getCount(), null);
                collector.differentialMetric("jdbc-queries", "db-queries", "JDBC Queries", numQueries.getCount(), null);
                collector.metric("db-query-duration", "JDBC Query Duration", queryDuration.getAndClearAverage(), "ms");
            }
        }
    }

    /**
     * Provides access to the selected database.
     * <p>
     * The configuration of the connection pool will be loaded from <tt>jdbc.database.[name]</tt>
     * </p>
     *
     * @param name name of the database to access
     * @return a wrapper providing access to the given database
     */
    public static Database get(String name) {
        Database ds = datasources.get(name);
        if (ds == null) {
            synchronized (datasources) {
                ds = datasources.get(name);
                if (ds == null) {
                    ds = new Database(name);
                    datasources.put(name, ds);
                }
            }
        }
        return ds;
    }

    /*
     * Use the get(name) method to create a new object.
     */
    private Database(String name) {
        Extension ext = Extensions.getExtension("jdbc.database", name);
        Extension profile = Extensions.getExtension("jdbc.profile", ext.get("profile").asString("default"));
        Context ctx = profile.getContext();
        ctx.putAll(ext.getContext());
        this.name = name;
        this.driver = ext.get("driver").isEmptyString() ? Formatter.create(profile.get("driver").asString())
                                                                   .set(ctx)
                                                                   .format() : ext.get("driver").asString();
        this.url = ext.get("url").isEmptyString() ? Formatter.create(profile.get("url").asString())
                                                             .set(ctx)
                                                             .format() : ext.get("url").asString();
        this.username = ext.get("user").isEmptyString() ? Formatter.create(profile.get("user").asString())
                                                                   .set(ctx)
                                                                   .format() : ext.get("user").asString();
        this.password = ext.get("password").isEmptyString() ? Formatter.create(profile.get("password").asString())
                                                                       .set(ctx)
                                                                       .format() : ext.get("password").asString();
        this.initialSize = ext.get("initialSize").isFilled() ? ext.get("initialSize").asInt(0) : profile.get(
                "initialSize").asInt(0);
        this.maxActive = ext.get("maxActive").isFilled() ? ext.get("maxActive").asInt(10) : profile.get("maxActive")
                                                                                                   .asInt(10);
        this.maxIdle = ext.get("maxIdle").isFilled() ? ext.get("maxIdle").asInt(1) : profile.get("maxIdle").asInt(1);
        this.validationQuery = ext.get("validationQuery").isEmptyString() ? Formatter.create(profile.get(
                "validationQuery").asString()).set(ctx).format() : ext.get("validationQuery").asString();
        this.testOnBorrow = Strings.isFilled(validationQuery);
    }

    /**
     * Provides access to the underlying {@link DataSource} representing the connection pool.
     * <p>
     * You must ensure to close each opened connection property as otherwise the pool will lock up, once all
     * connections are busy. Consider using {@link #createQuery(String)} or
     * {@link #createFunctionCall(String, Integer)} or {@link #createProcedureCall(String)} to access the database
     * in a safe manner.
     * </p>
     *
     * @return the connection pool as DataSource
     */
    public DataSource getDatasource() {
        if (ds == null) {
            ds = new BasicDataSource();
            initialize();
        }
        return ds;
    }

    /**
     * Creates a new connection to the database.
     * <p>
     * You must ensure to close each opened connection property as otherwise the pool will lock up, once all
     * connections are busy. Consider using {@link #createQuery(String)} or
     * {@link #createFunctionCall(String, Integer)} or {@link #createProcedureCall(String)} to access the database
     * in a safe manner.
     * </p>
     *
     * @return a new {@link Connection} to the database
     * @throws SQLException in case of a database error
     */
    public Connection getConnection() throws SQLException {
        return new WrappedConnection(getDatasource().getConnection(), this);
    }

    /**
     * Creates a new query wrapper which permits safe and convenient queries.
     * <p>
     * Using this wrapper ensures proper connection handling and simplifies query creation.
     * </p>
     *
     * @param sql the SQL to send to the database. For syntax help concerning parameter names and optional query parts,
     *            see {@link JDBCQuery}
     * @return a new query which can be supplied with parameters and executed against the database
     * @see JDBCQuery
     */
    public JDBCQuery createQuery(String sql) {
        return new JDBCQuery(this, sql);
    }

    /**
     * Creates a new call wrapper which permits safe and convenient function calls.
     *
     * @param fun        name of the function to call
     * @param returnType the SQL type ({@link Types}) of the return value of this function
     * @return a new call which can be supplied with parameters and executed against the database
     */
    public JDBCCall createFunctionCall(String fun, Integer returnType) {
        return new JDBCCall(this, fun, returnType);
    }

    /**
     * Creates a new call wrapper which permits safe and convenient procedure calls.
     *
     * @param fun name of the procedure to call
     * @return a new call which can be supplied with parameters and executed against the database
     */
    public JDBCCall createProcedureCall(String fun) {
        return new JDBCCall(this, fun, null);
    }

    /**
     * Generates an INSERT statement for the given table inserting all supplied parameters in <tt>ctx</tt>.
     *
     * @param table the target table to insert a row
     * @param ctx   the parameter names and values to insert into the database
     * @return a Row containing all generated keys
     * @throws SQLException in case of a database error
     */
    public Row insertRow(String table, Context ctx) throws SQLException {
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
            PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
                ResultSet rs = stmt.getGeneratedKeys();
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

    /**
     * Returns the JDBC connection URL
     *
     * @return the JDBC connection URL used to connect to the database
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the maximal number of concurrent connections
     *
     * @return the maximal number of concurrent connections
     */
    public int getSize() {
        return maxActive;
    }

    /**
     * Return the number of idle connections
     *
     * @return the number of open but unused connection
     */
    public int getNumIdle() {
        if (ds == null) {
            return 0;
        }
        return ds.getNumIdle();
    }

    /**
     * Return the number of active connections
     *
     * @return the number of open and active connection
     */
    public int getNumActive() {
        if (ds == null) {
            return 0;
        }
        return ds.getNumActive();
    }

    /**
     * Determines if the target database is MySQL
     *
     * @return <tt>true</tt> if the target database is MySQL, <tt>false</tt> otherwise
     */
    public boolean isMySQL() {
        return "com.mysql.jdbc.Driver".equalsIgnoreCase(driver);
    }

    @Override
    public String toString() {
        return Strings.apply("%s (%d/%d)", name, getNumActive(), getSize());
    }
}
