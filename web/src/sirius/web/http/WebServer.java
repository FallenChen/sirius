package sirius.web.http;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.DiskAttribute;
import org.jboss.netty.handler.codec.http.multipart.DiskFileUpload;
import org.jboss.netty.handler.codec.http.multipart.HttpDataFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Register
public class WebServer implements Lifecycle {

    @ConfigValue("http.port")
    private int port;

    @ConfigValue("http.uploadDiskThreshold")
    private long uploadDiskThreshold;

    @ConfigValue("http.minUploadFreespace")
    private static long minUploadFreespace;

    @ConfigValue("http.maxUploadSize")
    private static long maxUploadSize;

    @Context
    private GlobalContext ctx;

    public static final Log LOG = Log.get("web");
    private static HttpDataFactory httpDataFactory;
    private ThreadPoolExecutor bossExecutor;
    private ThreadPoolExecutor workerExecutor;
    private ServerBootstrap bootstrap;


    protected static long getMinUploadFreespace() {
        return minUploadFreespace;
    }

    protected static long getMaxUploadSize() {
        return maxUploadSize;
    }

    protected static HttpDataFactory getHttpDataFactory() {
        return httpDataFactory;
    }

    @Override
    public void started() {
        if (port <= 0) {
            LOG.INFO("web server is disabled (http.port is <= 0)");
            return;
        }
        LOG.INFO("Initializing netty at port %d", port);

        // Setup disk handling.
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
        httpDataFactory = new DefaultHttpDataFactory(uploadDiskThreshold);

        // Keep our sexy thread names..
        ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);

        // Std-Header
        // Web-stats
        // Web-Timing
        // Service-Stats
        // Bind to request
        // @Description
        // Help-System
        // I18n-System
        // Async-Engine
        // JDBC

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
