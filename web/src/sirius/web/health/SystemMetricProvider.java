package sirius.web.health;

import sirius.kernel.commons.Collector;
import sirius.kernel.di.std.Register;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
@Register
public class SystemMetricProvider implements MetricProvider {

    @Override
    public void gather(Collector<Metric> collector) {

    }

}
