/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.schema;

import java.util.Collections;
import java.util.List;

/**
 * Represents an action which needs to be perfomed onto a schema to make it
 * match another schema.
 *
 * @author aha
 */
public class SchemaUpdateAction {
    private String reason;
    private List<String> sql;
    private boolean dataLossPossible;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = Collections.singletonList(sql);
    }

    public void setSql(List<String> sql) {
        this.sql = sql;
    }

    public boolean isDataLossPossible() {
        return dataLossPossible;
    }

    public void setDataLossPossible(boolean dataLossPossible) {
        this.dataLossPossible = dataLossPossible;
    }

}
