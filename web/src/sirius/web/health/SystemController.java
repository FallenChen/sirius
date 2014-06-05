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

/**
 * Contains the default admin GUI.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/01
 */
@Register(classes = Controller.class)
public class SystemController implements Controller {

    /**
     * Used to retrieve a factory (via {@link sirius.kernel.di.GlobalContext#make(Class, String)} which checks access
     * to /system/console.
     */
    public static final String SYSTEM_CONSOLE_ACCESS_CHECKER = "SystemController.SYSTEM_CONSOLE_ACCESS_CHECKER";

    /**
     * Used to retrieve a factory (via {@link sirius.kernel.di.GlobalContext#make(Class, String)} which checks access
     * to /system/logs.
     */
    public static final String SYSTEM_LOGS_ACCESS_CHECKER = "SystemController.SYSTEM_LOGS_ACCESS_CHECKER";

    /**
     * Used to retrieve a factory (via {@link sirius.kernel.di.GlobalContext#make(Class, String)} which checks access
     * to /system/errors.
     */
    public static final String SYSTEM_ERRORS_ACCESS_CHECKER = "SystemController.SYSTEM_ERRORS_ACCESS_CHECKER";

    @Routed("/system/console")
    public void console(WebContext ctx) {
        if (context.make(Boolean.class, SYSTEM_CONSOLE_ACCESS_CHECKER).orElse(true)) {
            ctx.respondWith().cached().template("/view/system/console.html");
        } else {
            ctx.respondWith().error(HttpResponseStatus.FORBIDDEN);
        }
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
    public void logs(WebContext ctx) {
        if (context.make(Boolean.class, SYSTEM_LOGS_ACCESS_CHECKER).orElse(true)) {
            ctx.respondWith().template("/view/system/logs.html", monitor.getMessages());
        } else {
            ctx.respondWith().error(HttpResponseStatus.FORBIDDEN);
        }
    }

    @Routed("/system/errors")
    public void errors(WebContext ctx) {
        if (context.make(Boolean.class, SYSTEM_ERRORS_ACCESS_CHECKER).orElse(true)) {
            ctx.respondWith().template("/view/system/errors.html", monitor.getIncidents());
        } else {
            ctx.respondWith().error(HttpResponseStatus.FORBIDDEN);
        }
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
    public void state(WebContext ctx) {
        ctx.respondWith().template("/view/system/state.html", cluster, metrics, ctx.get("all").asBoolean(false));
    }

}
