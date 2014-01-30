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
 * User: aha
 * Date: 30.12.13
 * Time: 15:21
 * To change this template use File | Settings | File Templates.
 */
public interface MetricsCollector {

    void metric(String limitType, String title, double value, String unit);

    void metric(String title, double value, String unit, MetricState state);

    void differentialMetric(String id, String limitType, String title, double currentValue, String unit);
}
