package sirius.web.health;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public interface MetricProvider {
    void gather(MetricsCollector collector);
}
