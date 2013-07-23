package sirius.web;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.annotations.Context;
import sirius.kernel.di.annotations.Part;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.health.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Register
public class WebServer implements Lifecycle {

    @Part
    private Config config;

    @Context
    private GlobalContext ctx;

    public static final Log LOG = Log.get("web");
    private ThreadPoolExecutor bossExecutor;
    private ThreadPoolExecutor workerExecutor;
    private ServerBootstrap bootstrap;

    @Override
    public void started() {
        int port = config.getInt("http.port");
        if (port <= 0) {
            LOG.INFO("web server is disabled (http.port is <= 0)");
            return;
        }
        LOG.INFO("Initializing netty at port %d", port);

        // Keep our sexy thread names..
        ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);

        // 1) CTX durchreichen!
        // 1.2) health in WEB

        // 1) Service-Dispatcher
        // 2) Help-Dispatcher
        // 3) Stats
        // 4) Console
        // 5) langs
        // 6) GUI
        // 7) Servlet-Container
        //> @prefix @config
        // Std-Importe
        // Config-Mapping - System-Description

        // Configure the server.
        bossExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        bossExecutor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("http-boss-%d").build());
        workerExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        workerExecutor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("http-%d").build());
        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(bossExecutor, workerExecutor));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(ctx.wire(new WebServerPipelineFactory()));

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));
    }

    @Override
    public void stopped() {
        bootstrap.shutdown();
    }

    @Override
    public String getName() {
        return "web (netty HTTP Server)";
    }
}
