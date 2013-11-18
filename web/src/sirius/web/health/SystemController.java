package sirius.web.health;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.MemoryBasedHealthMonitor;
import sirius.web.controller.Controller;
import sirius.web.controller.Routed;
import sirius.web.http.WebContext;

/**
 * Contains the default admin GUI.
 */
@Register(classes = Controller.class)
public class SystemController implements Controller {

    @Routed("/system/console")
    public void console(WebContext ctx) {
        ctx.respondWith().cached().template("/view/system/console.html");
    }

    @Override
    public void onError(WebContext ctx, HandledException error) {
        ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
    }

    @Part
    private MemoryBasedHealthMonitor monitor;

    @Routed("/system/logs")
    public void logs(WebContext ctx) {
        ctx.respondWith().template("/view/system/logs.html", monitor.getMessages());
    }

    @Routed("/system/errors")
    public void errors(WebContext ctx) {
        ctx.respondWith().template("/view/system/logs.html", monitor.getIncidents());
    }

}
