/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Collector;
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
                    public void metric(String title, double value, String unit, MetricState state) {
                        collector.add(new Metric(title, value, state, unit));
                    }

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

    private class Limit {
        double gray = 0;
        double yellow = 0;
        double red = 0;

        @Override
        public String toString() {
            return "(" + gray + " / " + yellow + " / " + red + ")";
        }
    }

    private MetricState computeState(String limitType, double value) {
        Limit limit = limits.get(limitType);
        if (limit == null) {
            limit = new Limit();
            if (Sirius.getConfig().hasPath("health.limits." + limitType + ".gray")) {
                limit.gray = Sirius.getConfig().getDouble("health.limits." + limitType + ".gray");
            }
            if (Sirius.getConfig().hasPath("health.limits." + limitType + ".warning")) {
                limit.yellow = Sirius.getConfig().getDouble("health.limits." + limitType + ".warning");
            }
            if (Sirius.getConfig().hasPath("health.limits." + limitType + ".error")) {
                limit.red = Sirius.getConfig().getDouble("health.limits." + limitType + ".error");
            }
            limits.put(limitType, limit);
        }
        if (limit.gray > 0 && value <= limit.gray) {
            return MetricState.GRAY;
        }
        if (limit.red > 0 && value >= limit.red) {
            return MetricState.RED;
        }
        if (limit.yellow > 0 && value >= limit.yellow) {
            return MetricState.YELLOW;
        }
        return MetricState.GREEN;
    }

    @Parts(MetricProvider.class)
    private Collection<MetricProvider> providers;
    private List<Metric> metrics = Lists.newArrayList();
    private Map<String, Limit> limits = Maps.newHashMap();
    private Map<String, Double> differentials = Maps.newHashMap();

    public List<Metric> getMetrics() {
        return Collections.unmodifiableList(metrics);
    }


}
