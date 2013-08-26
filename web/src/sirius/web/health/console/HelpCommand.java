package sirius.web.health.console;

import sirius.kernel.Sirius;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Register;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 28.07.13
 * Time: 19:37
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "help")
public class HelpCommand implements Command {
    @Context
    private GlobalContext ctx;

    @Override
    public void execute(Output output, String... params) throws Exception {
        output.blankLine();
        output.apply("C O N S O L E  -  %s / %s", Sirius.getProductName(), Sirius.getProductVersion());
        output.blankLine();
        output.apply("%-10s %s", "CMD", "DESCRIPTION");
        output.separator();
        for (Command cmd : ctx.getParts(Command.class)) {
            output.apply("%-10s %s", cmd.getName(), cmd.getDescription());
        }
        output.separator();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Generates this help screen.";
    }
}
