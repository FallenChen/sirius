/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.health;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.MemoryBasedHealthMonitor;
import sirius.web.controller.Controller;
import sirius.web.controller.Routed;
import sirius.web.http.MimeHelper;
import sirius.web.http.WebContext;

/**
 * Contains the default admin GUI and some default responses (e.g. robots.txt).
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

    @Part
    private Cluster cluster;

    @Part
    private Metrics metrics;

    @Routed("/system/logs")
    public void logs(WebContext ctx) {
        ctx.respondWith().template("/view/system/logs.html", monitor.getMessages());
    }

    @Routed("/system/errors")
    public void errors(WebContext ctx) {
        ctx.respondWith().template("/view/system/errors.html", monitor.getIncidents());
    }

    @Routed("/system/ok")
    public void ok(WebContext ctx) {
        ctx.respondWith().status(HttpResponseStatus.OK);
    }

    @Routed("/system/fail")
    public void fail(WebContext ctx) {
        Object n = null;
        n.toString();
    }

    @Routed("/system/info")
    public void info(WebContext ctx) {
        ctx.respondWith().template("/view/system/info.html", monitor.getIncidents());
    }

    @Routed("/system/state")
    public void state(WebContext ctx) {
        ctx.respondWith().template("/view/system/state.html", cluster, metrics, ctx.get("all").asBoolean(false));
    }

    @Routed("/crossdomain.xml")
    public void crossdomain(WebContext ctx) {
        ctx.respondWith()
           .infinitelyCached()
           .setHeader(HttpHeaders.Names.CONTENT_TYPE, MimeHelper.TEXT_XML)
           .direct(HttpResponseStatus.OK, "<?xml version=\"1.0\"?>\n" +
                   "<!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\">\n" +
                   "<cross-domain-policy>\n" +
                   "    <site-control permitted-cross-domain-policies=\"all\" />\n" +
                   "    <allow-access-from domain=\"*\" secure=\"false\" />\n" +
                   "    <allow-http-request-headers-from domain=\"*\" headers=\"*\"/>\n" +
                   "</cross-domain-policy>");
    }

    @Routed("/robots.txt")
    public void robots(WebContext ctx) {
        ctx.respondWith().template("/view/system/status.html", monitor.getIncidents());
    }

}
