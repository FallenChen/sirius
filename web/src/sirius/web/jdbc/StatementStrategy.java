/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.jdbc;

import java.sql.SQLException;

public interface StatementStrategy {
    void appendString(String queryPart);

    void set(int idx, Object value) throws SQLException;

    String getParameterName();

    String getQueryString();
}
