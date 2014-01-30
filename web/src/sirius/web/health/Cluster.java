/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.joda.time.DateTime;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.timer.EveryMinute;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.12.13
 * Time: 16:22
 * To change this template use File | Settings | File Templates.
 */
@Register(classes = {Cluster.class, EveryMinute.class})
public class Cluster implements EveryMinute {

    public static final Log LOG = Log.get("cluster");

    private MetricState nodeState = MetricState.GREEN;
    private MetricState clusterState = MetricState.GREEN;
    private List<NodeInfo> nodes = null;

    @ConfigValue("health.cluster.priority")
    private int priority;

    @Part
    private Metrics metrics;

    public List<NodeInfo> getNodeInfos() {
        if (nodes == null) {
            List<NodeInfo> result = Lists.newArrayList();
            for (String endpoint : Sirius.getConfig().getStringList("health.cluster.nodes")) {
                NodeInfo info = new NodeInfo();
                info.setEndpoint(endpoint);
                result.add(info);
            }
            nodes = result;
        }

        return nodes;
    }

    public NodeInfo getBestAvailableNode() {
        for (NodeInfo info : nodes) {
            if (info.getPriority() < priority && info.getNodeState() == MetricState.GREEN) {
                return info;
            }
        }

        return null;
    }

    public boolean isBestAvailableNode() {
        for (NodeInfo info : nodes) {
            if (info.getPriority() < priority && info.getNodeState() == MetricState.GREEN) {
                return false;
            }
        }

        return true;
    }

    public MetricState getNodeState() {
        return nodeState;
    }

    protected void setNodeState(MetricState state) {
        this.nodeState = state;
    }

    public MetricState getClusterState() {
        return clusterState;
    }

    @Override
    public void runTimer() throws Exception {
        // Compute local state
        MetricState newNodeState = MetricState.GREEN;
        for (Metric m : metrics.getMetrics()) {
            if (m.getState().ordinal() > newNodeState.ordinal()) {
                newNodeState = m.getState();
            }
        }
        setNodeState(newNodeState);

        // Compute cluster state
        MetricState newClusterState = newNodeState;
        LOG.FINE("Scanning cluster...");
        for (NodeInfo info : getNodeInfos()) {
            try {
                LOG.FINE("Testing node: %s", info.getEndpoint());
                URLConnection c = new URL(info.getEndpoint() + "/service/json/system/node-info").openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setDoInput(true);
                c.setDoOutput(false);
                try (InputStream in = c.getInputStream()) {
                    JSONObject response = JSON.parseObject(CharStreams.toString(new InputStreamReader(in,
                                                                                                      Charsets.UTF_8)));
                    info.setName(response.getString("name"));
                    info.setNodeState(MetricState.valueOf(response.getString("nodeState")));
                    if (info.getNodeState().ordinal() > newClusterState.ordinal()) {
                        newClusterState = info.getNodeState();
                    }
                    info.setClusterState(MetricState.valueOf(response.getString("clusterState")));
                    info.setPriority(response.getInteger("priority"));
                    info.setUptime(response.getString("uptime"));
                    info.setLastPing(new DateTime());
                    info.getMetrics().clear();
                    JSONArray metrics = response.getJSONArray("metrics");
                    for (int i = 0; i < metrics.size(); i++) {
                        try {
                            JSONObject metric = (JSONObject) metrics.get(i);
                            Metric m = new Metric(metric.getString("name"),
                                                  metric.getDoubleValue("value"),
                                                  MetricState.valueOf(metric.getString("state")),
                                                  metric.getString("unit"));
                            info.getMetrics().add(m);
                        } catch (Throwable e) {
                            // Ignore non-well-formed metrics...
                            LOG.FINE(e);
                        }
                    }
                    info.resetPingFailures();
                    LOG.FINE("Node: %s is %s (%s)", info.getName(), info.getNodeState(), info.getClusterState());
                }
            } catch (Throwable t) {
                Exceptions.handle(LOG, t);
                info.setNodeState(MetricState.RED);
                info.setClusterState(MetricState.RED);
                newClusterState = MetricState.RED;
                info.incPingFailures();
            }
        }

        // Since the cluster.nodes array might contain all nodes of the cluster, we filter out or own (by name)
        Iterator<NodeInfo> iter = getNodeInfos().iterator();
        while (iter.hasNext()) {
            if (Strings.areEqual(CallContext.getNodeName(), iter.next().getName())) {
                iter.remove();
            }
        }

        // Check cluster state
        if (clusterState == MetricState.RED && newClusterState == MetricState.RED) {
            LOG.FINE("Cluster was RED and remained RED - ensuring alert...");
            ensureAlertClusterFailure();
        }
        LOG.FINE("Cluster check complete. Status was %s and is now %s", clusterState, newClusterState);
        clusterState = newClusterState;
    }

    private void ensureAlertClusterFailure() {
        if (!isBestAvailableNode()) {
            // Check if a node with a better priority also detected the cluster failure and considers itself GREEN
            for (NodeInfo info : getNodeInfos()) {
                if (info.getPriority() < getPriority() &&
                        info.getClusterState() == MetricState.RED &&
                        info.getNodeState() == MetricState.GREEN) {
                    // Another node took care of it...
                    LOG.FINE("Node %s is in charge of sending an alert", info.getName());
                    return;
                }
            }
        }
        LOG.FINE("This node is in charge of action at the bell....fire alert!");
        alertClusterFailure();
    }

    private void alertClusterFailure() {
        // TODO PANIC REAL HARD!
        LOG.SEVERE("Cluster is RED!");

    }

    public int getPriority() {
        return priority;
    }
}
