package sirius.web.health.console;

import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;
import sirius.web.http.WebServer;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 28.07.13
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "http")
public class HTTPCommand implements Command {


    @Override
    public void execute(Output output, String... params) throws Exception {
        output.apply("%-20s %10s", "NAME", "VALUE");
        output.separator();
        output.apply("%-20s %10s", "Bytes In", NLS.formatSize(WebServer.getBytesIn()));
        output.apply("%-20s %10s", "Bytes Out", NLS.formatSize(WebServer.getBytesOut()));
        output.apply("%-20s %10d", "Packets In", WebServer.getMessagesIn());
        output.apply("%-20s %10d", "Packets Out", WebServer.getMessagesOut());
        output.apply("%-20s %10d", "Connects", WebServer.getConnections());
        output.apply("%-20s %10d", "Blocked Connects", WebServer.getBlockedConnections());
        output.apply("%-20s %10d", "Requests", WebServer.getRequests());
        output.apply("%-20s %10d", "Chunks", WebServer.getChunks());
        output.apply("%-20s %10d", "Keepalives", WebServer.getKeepalives());
        output.apply("%-20s %10d", "Open Connections", WebServer.getOpenConnections().get());
        output.apply("%-20s %10d", "Idle Timeouts", WebServer.getIdleTimeouts());
        output.apply("%-20s %10d", "Client Errors", WebServer.getClientErrors());
        output.apply("%-20s %10d", "Server Errors", WebServer.getServerErrors());
        output.apply("%-20s %10d", "Avg. Response Time", NLS.toUserString(WebServer.getAvgResponseTime()) + " ms");
        output.separator();
    }

    @Override
    public String getName() {
        return "http";
    }

    @Override
    public String getDescription() {
        return "Reports statistics for the web server";
    }
}
