package sirius.web.health;

import com.google.common.collect.Lists;
import sirius.kernel.commons.Collector;
import sirius.kernel.commons.MultiMap;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.timer.EveryTenSeconds;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
@Register(classes = {Metrics.class, EveryTenSeconds.class})
public class Metrics implements EveryTenSeconds {

    @Override
    public void runTimer() throws Exception {
        Collector<Metric> collector = Collector.create();
        for (MetricProvider provider : providers) {
            provider.gather(collector);
        }
        List<Metric> metricsList = collector.getData();
        Collections.sort(metricsList);
        MultiMap<String, Metric> newMetrics = MultiMap.create();
        for (Metric metric : metricsList) {
            newMetrics.put(metric.getCategory(), metric);
        }
        metrics = newMetrics;
    }

    public static enum MetricState {
        GREEN, YELLOW, RED;
    }

    public static final String MAIN_CATEGORY = "Main";

    @Parts(MetricProvider.class)
    private Collection<MetricProvider> providers;
    private MultiMap<String, Metric> metrics = MultiMap.create();

    public Collection<Metric> getLocalMainMetrics() {
        return metrics.get(MAIN_CATEGORY);
    }

    public Collection<String> getLocalMetricCategories() {
        List<String> result = Lists.newArrayList(metrics.keySet());
        result.remove(MAIN_CATEGORY);
        return result;
    }

    public Collection<Metric> getLocalMetrics(String category) {
        return metrics.get(category);
    }


}
