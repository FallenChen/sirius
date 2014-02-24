package sirius.search;

import sirius.kernel.di.std.Register;
import sirius.web.health.console.Command;

@Register(name = "reindex")
public class ReIndexCommand implements Command {
    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length !=1) {
            output.line("Usage: reindex newIndex");
        } else {
            Index.getSchema().reIndex(params[0]);
            output.line("Operation has started!");
        }
    }

    @Override
    public String getName() {
        return "reindex";
    }

    @Override
    public String getDescription() {
        return "Runs an re-index on ElasticSearch";
    }
}
