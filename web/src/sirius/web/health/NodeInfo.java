/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.12.13
 * Time: 16:25
 * To change this template use File | Settings | File Templates.
 */
public class NodeInfo {
    private String name;
    private int priority;
    private String endpoint;
    private DateTime lastPing;
    private int pingFailures;
    private String uptime;
    private MetricState nodeState;
    private MetricState clusterState;
    private List<Metric> metrics = Lists.newArrayList();


    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    protected void setPriority(int priority) {
        this.priority = priority;
    }

    public String getEndpoint() {
        return endpoint;
    }

    protected void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public DateTime getLastPing() {
        return lastPing;
    }

    protected void setLastPing(DateTime lastPing) {
        this.lastPing = lastPing;
    }

    public MetricState getNodeState() {
        return nodeState;
    }

    protected void setNodeState(MetricState nodeState) {
        this.nodeState = nodeState;
    }

    public MetricState getClusterState() {
        return clusterState;
    }

    protected void setClusterState(MetricState clusterState) {
        this.clusterState = clusterState;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public int getPingFailures() {
        return pingFailures;
    }

    public void incPingFailures() {
        this.pingFailures++;
    }

    public void resetPingFailures() {
        this.pingFailures = 0;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }
}
