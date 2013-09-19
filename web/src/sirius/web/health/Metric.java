package sirius.web.health;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class Metric implements Comparable<Metric> {
    private String category;
    private String name;
    private String value;
    private Metrics.MetricState state;

    public Metric(String category, String name, String value, Metrics.MetricState state) {
        this.category = category;
        this.name = name;
        this.value = value;
        this.state = state;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Metrics.MetricState getState() {
        return state;
    }

    @Override
    public int compareTo(Metric o) {
        if (o == null) {
            return -1;
        }
        if (o == this) {
            return 0;
        }
        if (o.state != state) {
            return o.state.ordinal() - state.ordinal();
        }
        return name.compareTo(o.name);
    }
}
