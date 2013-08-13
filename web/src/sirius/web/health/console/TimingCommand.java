package sirius.web.health.console;

import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Average;
import sirius.kernel.health.Microtiming;

@Register(name = "timing")
public class TimingCommand implements Command {

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 1 && Strings.isFilled(params[0])) {
            if ("enable".equalsIgnoreCase(params[0]) || "+".equalsIgnoreCase(params[0])) {
                if (Microtiming.isEnabled()) {
                    generateOutput(output);
                    Microtiming.setEnabled(false);
                    output.line("Resetting Microtiming...");
                } else {
                    output.line("Enabling Microtiming...");
                }
                Microtiming.setEnabled(true);
            } else if ("disable".equalsIgnoreCase(params[0]) || "-".equalsIgnoreCase(params[0])) {
                generateOutput(output);
                Microtiming.setEnabled(false);
                output.line("Disabling Microtiming...");
            } else {
                output.line("Usage: timing enable|disable (You can use + and - for enable/disable).");
                output.line("To enable tracing: timing trace <filter-expression>");
            }
        } else {
            if (Microtiming.isEnabled()) {
                generateOutput(output);
            } else {
                output.line("Microtiming is disabled! Use: 'timing +' to enable.");
            }
        }
    }

    protected void generateOutput(Output output) {
        long delta = System.currentTimeMillis() - Microtiming.getLastReset();
        output.apply("%8s %9s %5s %5s %s", "AVG[ms]", "TOTAL[ms]", "RATIO", "COUNT", "NAME");
        output.separator();
        for (Tuple<String, Average> timing : Microtiming.getTimings()) {
            double totalTime = timing.getSecond().getAvg() / 1000d * timing.getSecond().getCount();
            double percentTime = (totalTime * 100d) / delta;
            output.apply("%8.2f %9d %4.2f%% %5d %s",
                         timing.getSecond().getAvg() / 1000d,
                         Math.round(totalTime),
                         percentTime,
                         timing.getSecond().getCount(),
                         timing.getFirst());
        }
        output.separator();
    }

    @Override
    public String getName() {
        return "timing";
    }

    @Override
    public String getDescription() {
        return "Reports statistics recorded by the micro timer";
    }

}
