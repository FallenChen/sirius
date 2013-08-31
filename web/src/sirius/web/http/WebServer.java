/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.DiskAttribute;
import org.jboss.netty.handler.codec.http.multipart.DiskFileUpload;
import org.jboss.netty.handler.codec.http.multipart.HttpDataFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Responsible for setting up and starting netty as HTTP server.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Register
public class WebServer implements Lifecycle {

    /**
     * Used to log all web / http relevant messages
     */
    public static final Log LOG = Log.get("web");

    /**
     * Config value of the HTTP port used (<tt>http.port</tt>). The default port for HTTP is 80, but the default value
     * here is 9000 because non root users cannot open ports less than 1024
     */
    @ConfigValue("http.port")
    private int port;

    /**
     * Config value of the HTTP bind address used (<tt>http.bindAddress</tt>). If the value is empty, we bind all
     * addresses. Otherwise this can be used to bind against a single IP address in order to setup multiple servers
     * on the same port.
     */
    @ConfigValue("http.bindAddress")
    private String bindAddress;

    /**
     * Config value of the max size for uploads which are kept entirely in memory (<tt>"http.uploadDiskThreshold</tt>).
     */
    @ConfigValue("http.uploadDiskThreshold")
    private long uploadDiskThreshold;

    /**
     * Config value of the min free space on disk (<tt>"http.minUploadFreespace</tt>). If the free space on disk drops
     * below this value, we cancel an upload. This provides large uploads which never crash a system by jamming a
     * disk.
     */
    @ConfigValue("http.minUploadFreespace")
    private static long minUploadFreespace;

    /**
     * Config value of the max upload size (<tt>http.maxUploadSize</tt>). Can be used to specify an upper limit for
     * file uploads.
     */
    @ConfigValue("http.maxUploadSize")
    private static long maxUploadSize;

    @Context
    private GlobalContext ctx;

    private static HttpDataFactory httpDataFactory;
    private ThreadPoolExecutor bossExecutor;
    private ThreadPoolExecutor workerExecutor;
    private ServerBootstrap bootstrap;
    private NioServerSocketChannelFactory channelFactory;
    private HashedWheelTimer timer;

    /**
     * Returns the minimal value of free disk space accepted until an upload is aborted.
     *
     * @return the minimal allowed free disk space in bytes
     */
    protected static long getMinUploadFreespace() {
        return minUploadFreespace;
    }

    /**
     * Returns the maximal upload size in bytes
     *
     * @return the max size of uploaded files
     */
    protected static long getMaxUploadSize() {
        return maxUploadSize;
    }

    /**
     * Returns the data factory used to handle file uploads and posts.
     *
     * @return the data factory used for processing POST and PUT requests.
     */
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
        if (Strings.isFilled(bindAddress)) {
            LOG.INFO("Binding netty to %s", bindAddress);
        }

        // Setup disk handling.
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
        httpDataFactory = new DefaultHttpDataFactory(uploadDiskThreshold);

        // Keep our sexy thread names..
        ThreadRenamingRunnable.setThreadNameDeterminer(ThreadNameDeterminer.CURRENT);

        // Configure the server.
        bossExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        bossExecutor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("http-boss-%d").build());
        workerExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        workerExecutor.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("http-%d").build());
        channelFactory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
        bootstrap = new ServerBootstrap(channelFactory);
        timer = new HashedWheelTimer();

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(ctx.wire(new WebServerPipelineFactory(timer)));

        // Bind and start to accept incoming connections.
        if (Strings.isFilled(bindAddress)) {
            bootstrap.bind(new InetSocketAddress(bindAddress, port));
        } else {
            bootstrap.bind(new InetSocketAddress(port));
        }
    }

    @Override
    public void stopped() {
        timer.stop();
        bootstrap.shutdown();
        channelFactory.shutdown();
        bossExecutor.shutdown();
        workerExecutor.shutdown();
        channelFactory.releaseExternalResources();
    }

    @Override
    public String getName() {
        return "web (netty HTTP Server)";
    }
}
