/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.xml.StructuredOutput;
import sirius.web.services.ServiceCall;
import sirius.web.services.StructuredService;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.12.13
 * Time: 16:47
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "system/node-info")
public class NodeInfoService implements StructuredService {

    @Part
    private Cluster cluster;

    @Override
    public void call(ServiceCall call, StructuredOutput out) throws Exception {
        out.beginResult();
        out.property("nodeState", cluster.getNodeState().toString());
        out.property("clusterState", cluster.getClusterState().toString());
        out.property("priority", cluster.getPriority());
        out.endResult();
    }
}
