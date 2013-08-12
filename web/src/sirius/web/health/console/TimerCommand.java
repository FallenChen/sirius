package sirius.web.health.console;

import sirius.kernel.di.annotations.Part;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.timer.TimerService;
import sirius.web.health.console.Command;

/**
 * Internal service which is responsible for executing timers.
 */
@Register(name = "timer")
public class TimerCommand implements Command {

    @Part
    private TimerService ts;

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 0) {
            output.line("Usage: timer all|oneMinute|tenMinutes|oneHour");
        } else {
            if ("all".equalsIgnoreCase(params[0]) || "oneMinute".equalsIgnoreCase(params[0])) {
                output.line("Executing one minute timers...");
                ts.runOneMinuteTimers();
            }
            if ("all".equalsIgnoreCase(params[0]) || "tenMinutes".equalsIgnoreCase(params[0])) {
                output.line("Executing ten minute timers...");
                ts.runTenMinuteTimers();
            }
            if ("all".equalsIgnoreCase(params[0]) || "oneHour".equalsIgnoreCase(params[0])) {
                output.line("Executing one hour timers...");
                ts.runOneHourTimers();
            }
        }
        output.blankLine();
        output.line("System Timers - Last Execution");
        output.separator();
        output.apply("%20s %-30s", "One-Minute", ts.getLastOneMinuteExecution());
        output.apply("%20s %-30s", "Ten-Minutes", ts.getLastTenMinutesExecution());
        output.apply("%20s %-30s", "One-Hour", ts.getLastHourExecution());
        output.separator();
    }

    @Override
    public String getName() {
        return "timer";
    }

    @Override
    public String getDescription() {
        return "Reports the last timer runs and executes them out of schedule.";
    }
}
