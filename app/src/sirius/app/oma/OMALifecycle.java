/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import sirius.app.oma.schema.SchemaUpdateAction;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 18.01.14
 * Time: 00:22
 * To change this template use File | Settings | File Templates.
 */
@Register(framework = "oma")
public class OMALifecycle implements Lifecycle {

    @ConfigValue("jdbc.oma.syncSchemaOnStartup")
    private boolean syncSchemaOnStartup;

    @Override
    public void started() {
        if (syncSchemaOnStartup) {
            for (SchemaUpdateAction sua : OMA.migrateSchema(false)) {
                if (!sua.isDataLossPossible()) {
                    OMA.LOG.INFO("Executing schema change: %s", sua.getReason());
                    for (String qry : sua.getSql()) {
                        try {
                            OMA.getDatabase().createQuery(qry).executeUpdate();
                        } catch (SQLException e) {
                            OMA.LOG.WARN("Schema change failed: %s", e.getMessage());
                        }
                    }
                } else {
                    OMA.LOG.WARN("Skipping schema change due to possible data loss: %s", sua.getReason());
                }
            }
        }
    }

    @Override
    public void stopped() {

    }

    @Override
    public void awaitTermination() {
        // NOOP
    }

    @Override
    public String getName() {
        return "oma";
    }
}
