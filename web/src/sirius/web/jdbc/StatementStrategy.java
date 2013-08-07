package sirius.web.jdbc;

import java.sql.SQLException;

public interface StatementStrategy {
    void appendString(String queryPart);

    void set(int idx, Object value) throws SQLException;

    String getParameterName();

    String getQueryString();
}
