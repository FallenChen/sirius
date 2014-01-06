package sirius.web.health;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Collector;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryMinute;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
@Register(classes = {Metrics.class, EveryMinute.class})
public class Metrics implements EveryMinute {

    @Override
    public void runTimer() throws Exception {
        synchronized (this) {
            final Collector<Metric> collector = Collector.create();
            for (MetricProvider provider : providers) {
                provider.gather(new MetricsCollector() {
                    @Override
                    public void metric(String limitType, String title, double value, String unit) {
                        collector.add(new Metric(title, value, computeState(limitType, value), unit));
                    }

                    @Override
                    public void differentialMetric(String id,
                                                   String limitType,
                                                   String title,
                                                   double currentValue,
                                                   String unit) {
                        Double lastValue = differentials.get(id);
                        if (lastValue != null) {
                            metric(limitType, title, currentValue - lastValue, unit);
                        }
                        differentials.put(id, currentValue);
                    }
                });
            }
            List<Metric> metricsList = collector.getData();
            Collections.sort(metricsList);
            metrics = metricsList;
        }
    }

    private MetricState computeState(String limitType, double value) {
        Tuple<Double, Double> limit = limits.get(limitType);
        if (limit == null) {
            limit = Tuple.create();
            if (Sirius.getConfig().hasPath("health.limits." + limitType + ".warning")) {
                limit.setFirst(Sirius.getConfig().getDouble("health.limits." + limitType + ".warning"));
                if (limit.getFirst() == 0d) {
                    limit.setFirst(null);
                }
            }
            if (Sirius.getConfig().hasPath("health.limits." + limitType + ".error")) {
                limit.setSecond(Sirius.getConfig().getDouble("health.limits." + limitType + ".error"));
                if (limit.getSecond() == 0d) {
                    limit.setSecond(null);
                }
            }
            limits.put(limitType, limit);
        }
        if (limit.getSecond() != null && value >= limit.getSecond()) {
            return MetricState.RED;
        }
        if (limit.getFirst() != null && value >= limit.getFirst()) {
            return MetricState.YELLOW;
        }
        return MetricState.GREEN;
    }

    public static enum MetricState {
        GREEN, YELLOW, RED;
    }

    @Parts(MetricProvider.class)
    private Collection<MetricProvider> providers;
    private List<Metric> metrics = Lists.newArrayList();
    private Map<String, Tuple<Double, Double>> limits = Maps.newHashMap();
    private Map<String, Double> differentials = Maps.newHashMap();

    public List<Metric> getMetrics() {
        return Collections.unmodifiableList(metrics);
    }


}
