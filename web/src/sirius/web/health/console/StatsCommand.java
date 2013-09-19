package sirius.web.health.console;

import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.web.health.Metric;
import sirius.web.health.Metrics;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 28.07.13
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "stats")
public class StatsCommand implements Command {

    @Part
    private Metrics metrics;

    @Override
    public void execute(Output output, String... params) throws Exception {
        output.apply("%-8s %-20s %10s", "STATE", "NAME", "VALUE");
        output.separator();
        for (Metric metric : metrics.getLocalMainMetrics()) {
            output.apply("%-8s %-20s %10s", metric.getState(), metric.getName(), metric.getValue());
        }
        output.separator();
        for (String category : metrics.getLocalMetricCategories()) {
            output.blankLine();
            output.line(category);
            output.separator();
            for (Metric metric : metrics.getLocalMetrics(category)) {
                output.apply("%-8s %-20s %10s", metric.getState(), metric.getName(), metric.getValue());
            }
            output.separator();
        }
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Reports all locally collected metrics of the system";
    }
}
