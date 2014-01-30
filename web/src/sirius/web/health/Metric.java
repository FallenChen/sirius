/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import sirius.kernel.commons.Amount;
import sirius.kernel.commons.NumberFormat;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class Metric implements Comparable<Metric> {
    private final String unit;
    private String name;
    private double value;
    private MetricState state;

    public Metric(String name, double value, MetricState state, String unit) {
        this.unit = unit;
        this.name = name;
        this.value = value;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getValueAsString() {
        return Amount.of(value).toSmartRoundedString(NumberFormat.TWO_DECIMAL_PLACES).append(" ", unit).toString();
    }

    public MetricState getState() {
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
