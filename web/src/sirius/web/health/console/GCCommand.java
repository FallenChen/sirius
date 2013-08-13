package sirius.web.health.console;

import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;

@Register(name = "gc")
public class GCCommand implements Command {

    @Override
    public void execute(Output output, String... params) throws Exception {
        output.apply("%-20s %10s", "TYPE", "SIZE");
        output.separator();
        output.apply("%-20s %10s", "Free", NLS.formatSize(Runtime.getRuntime().freeMemory()));
        output.apply("%-20s %10s", "Total", NLS.formatSize(Runtime.getRuntime().totalMemory()));
        output.apply("%-20s %10s", "Max", NLS.formatSize(Runtime.getRuntime().maxMemory()));
        Runtime.getRuntime().gc();
        output.separator();
        output.apply("%-20s %10s", "Free", NLS.formatSize(Runtime.getRuntime().freeMemory()));
        output.apply("%-20s %10s", "Total", NLS.formatSize(Runtime.getRuntime().totalMemory()));
        output.apply("%-20s %10s", "Max", NLS.formatSize(Runtime.getRuntime().maxMemory()));
        output.separator();
    }

    @Override
    public String getName() {
        return "gc";
    }

    @Override
    public String getDescription() {
        return "Invokes the garbage collector of the JVM";
    }

}
