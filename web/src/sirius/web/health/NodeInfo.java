package sirius.web.health;

import org.joda.time.DateTime;

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
    private Metrics.MetricState nodeState;
    private Metrics.MetricState clusterState;

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

    public Metrics.MetricState getNodeState() {
        return nodeState;
    }

    protected void setNodeState(Metrics.MetricState nodeState) {
        this.nodeState = nodeState;
    }

    public Metrics.MetricState getClusterState() {
        return clusterState;
    }

    protected void setClusterState(Metrics.MetricState clusterState) {
        this.clusterState = clusterState;
    }
}
