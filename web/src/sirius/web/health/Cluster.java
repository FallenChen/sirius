/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.joda.time.DateTime;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.timer.EveryMinute;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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

    private Metrics.MetricState nodeState = Metrics.MetricState.GREEN;
    private Metrics.MetricState clusterState = Metrics.MetricState.GREEN;
    private List<NodeInfo> nodes = null;

    @ConfigValue("health.cluster.priority")
    private int priority;

    public List<NodeInfo> getNodeInfos() {
        if (nodes == null) {
            List<NodeInfo> result = Lists.newArrayList();
            for (Extension ext : Extensions.getExtensions("health.cluster.nodes")) {
                NodeInfo info = new NodeInfo();
                info.setName(ext.getId());
                info.setEndpoint(ext.get("endpoint").asString());
                result.add(info);
            }
            nodes = result;
        }

        return nodes;
    }

    public NodeInfo getBestAvailableNode() {
        for (NodeInfo info : nodes) {
            if (info.getPriority() < priority && info.getNodeState() == Metrics.MetricState.GREEN) {
                return info;
            }
        }

        return null;
    }

    public boolean isBestAvailableNode() {
        for (NodeInfo info : nodes) {
            if (info.getPriority() < priority && info.getNodeState() == Metrics.MetricState.GREEN) {
                return false;
            }
        }

        return true;
    }

    public Metrics.MetricState getNodeState() {
        return nodeState;
    }

    protected void setNodeState(Metrics.MetricState state) {
        this.nodeState = state;
    }

    public Metrics.MetricState getClusterState() {
        return clusterState;
    }

    @Override
    public void runTimer() throws Exception {
        Metrics.MetricState newClusterState = getNodeState();
        LOG.FINE("Scanning cluster...");
        for (NodeInfo info : getNodeInfos()) {
            try {
                LOG.FINE("Testing node: %s", info.getName());
                URLConnection c = new URL(info.getEndpoint() + "/service/json/system/node-info").openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setDoInput(true);
                c.setDoOutput(false);
                try (InputStream in = c.getInputStream()) {
                    JSONObject response = JSON.parseObject(CharStreams.toString(new InputStreamReader(in,
                                                                                                      Charsets.UTF_8)));
                    info.setNodeState(Metrics.MetricState.valueOf(response.getString("nodeState")));
                    if (info.getNodeState().ordinal() > newClusterState.ordinal()) {
                        newClusterState = info.getNodeState();
                    }
                    info.setClusterState(Metrics.MetricState.valueOf(response.getString("clusterState")));
                    info.setPriority(response.getInteger("priority"));
                    info.setLastPing(new DateTime());
                    LOG.FINE("Node: %s is %s (%s)", info.getName(), info.getNodeState(), info.getClusterState());
                }
            } catch (Throwable t) {
                Exceptions.handle(LOG, t);
                newClusterState = Metrics.MetricState.RED;
            }
        }
        if (clusterState == Metrics.MetricState.RED && newClusterState == Metrics.MetricState.RED) {
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
                        info.getClusterState() == Metrics.MetricState.RED &&
                        info.getNodeState() == Metrics.MetricState.GREEN) {
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
