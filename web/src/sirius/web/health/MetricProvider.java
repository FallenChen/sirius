package sirius.web.health;

import sirius.kernel.commons.Collector;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public interface MetricProvider {
    void gather(Collector<Metric> collector);
}
