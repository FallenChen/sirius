package sirius.app.frameworks.kernel;

import sirius.app.oma.OMA;
import sirius.app.oma.schema.SchemaUpdateAction;
import sirius.app.vgui.TextOutApp;

import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 18:28
 * To change this template use File | Settings | File Templates.
 */

public class ExecSchemaChangesApp extends TextOutApp {
    @Override
    protected void execute() {
        for (SchemaUpdateAction sua : OMA.migrateSchema(false)) {
            println("Executing schema change: %s", sua.getReason());
            for (String qry : sua.getSql()) {
                try {
                    OMA.getDatabase().createQuery(qry).executeUpdate();
                } catch (SQLException e) {
                    println("Schema change failed: %s", e.getMessage());
                }
            }
        }
    }
}
