/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

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
