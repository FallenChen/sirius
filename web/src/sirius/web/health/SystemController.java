/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.MemoryBasedHealthMonitor;
import sirius.web.controller.Controller;
import sirius.web.controller.Routed;
import sirius.web.http.WebContext;
import sirius.web.security.Permission;

/**
 * Contains the default admin GUI.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
@Register(classes = Controller.class)
public class SystemController implements Controller {


    public static final String PERMISSION_SYSTEM_CONSOLE = "permission-system-console";

    public static final String PERMISSION_SYSTEM_LOGS = "permission-system-logs";

    public static final String PERMISSION_SYSTEM_ERRORS = "permission-system-errors";

    public static final String PERMISSION_SYSTEM_STATE = "permission-system-state";

    @Routed("/system/console")
    @Permission(PERMISSION_SYSTEM_CONSOLE)
    public void console(WebContext ctx) {
        ctx.respondWith().cached().template("/view/system/console.html");
    }

    @Override
    public void onError(WebContext ctx, HandledException error) {
        ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
    }

    @Part
    private MemoryBasedHealthMonitor monitor;

    @Part
    private Cluster cluster;

    @Part
    private Metrics metrics;

    @Context
    private GlobalContext context;

    @Routed("/system/logs")
    @Permission(PERMISSION_SYSTEM_LOGS)
    public void logs(WebContext ctx) {
        ctx.respondWith().template("/view/system/logs.html", monitor.getMessages());
    }

    @Routed("/system/errors")
    @Permission(PERMISSION_SYSTEM_ERRORS)
    public void errors(WebContext ctx) {
        ctx.respondWith().template("/view/system/errors.html", monitor.getIncidents());
    }

    @Routed("/system/ok")
    public void ok(WebContext ctx) {
        ctx.respondWith().status(HttpResponseStatus.OK);
    }

    /**
     * Can be used to forcefully create an error. (A NullPointerException in this case.)
     */
    @Routed("/system/fail")
    public void fail(WebContext ctx) {
        Object n = null;
        n.toString();
    }

    @Routed("/system/info")
    public void info(WebContext ctx) {
        ctx.respondWith().template("/view/system/info.html");
    }

    @Routed("/system/state")
    @Permission(PERMISSION_SYSTEM_STATE)
    public void state(WebContext ctx) {
        ctx.respondWith().template("/view/system/state.html", cluster, metrics, ctx.get("all").asBoolean(false));
    }

}
