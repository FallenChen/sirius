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
import sirius.kernel.commons.Amount;
import sirius.kernel.commons.Collector;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.health.Metric;
import sirius.web.health.MetricProvider;
import sirius.web.health.Metrics;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsible for setting up and starting netty as HTTP server.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
@Register
public class WebServer implements Lifecycle, MetricProvider {

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

    /**
     * Contains a list of IP ranges which are permitted to access this server. Access from unauthorized IPs will be
     * blocked at the lowest level possible (probably no connection will be accepted). The format accepted by this
     * field is defined by {@link IPRange#paraseRangeSet(String)}.
     *
     * @see IPRange#paraseRangeSet(String)
     */
    @ConfigValue("http.firewall.filterIPs")
    private static String ipFilter;
    private static IPRange.RangeSet filterRanges;

    /**
     * Returns an ip filter which determines which IPs may connect to the web server.
     *
     * @return a range set describing all accepted ranges
     */
    protected static IPRange.RangeSet getIPFilter() {
        if (filterRanges == null) {
            try {
                filterRanges = IPRange.paraseRangeSet(ipFilter);
            } catch (Throwable e) {
                Exceptions.handle()
                          .to(LOG)
                          .error(e)
                          .withSystemErrorMessage(
                                  "Error parsing config value: 'http.firewall.filterIPs': %s (%s). Defaulting to localhost!")
                          .handle();
                filterRanges = IPRange.LOCALHOST;
            }
        }

        return filterRanges;
    }

    /**
     * Contains a list of IP ranges which are trusted. This rule will not change the processing of requests, it will
     * only set the return value of {@link WebContext#isTrusted()}. The format accepted by this field is defined by
     * {@link IPRange#paraseRangeSet(String)}.
     *
     * @see IPRange#paraseRangeSet(String)
     */
    @ConfigValue("http.firewall.trustedIPs")
    private static String trustedIPs;
    private static IPRange.RangeSet trustedRanges;

    /**
     * Returns all trusted IPs as {@link IPRange.RangeSet}
     *
     * @return a range set describing all trusted ranges
     */
    protected static IPRange.RangeSet getTrustedRanges() {
        if (trustedRanges == null) {
            try {
                trustedRanges = IPRange.paraseRangeSet(trustedIPs);
                // If no trust range is given, we trust nobody but good old LOCALHOST....This seems to be a better
                // alternative than trusting everybody
                if (trustedRanges.isEmpty()) {
                    trustedRanges = IPRange.LOCALHOST;
                }
            } catch (Throwable e) {
                Exceptions.handle()
                          .to(LOG)
                          .error(e)
                          .withSystemErrorMessage("Error parsing config value: 'http.firewall.trustedIPs': %s (%s)")
                          .handle();
                trustedRanges = IPRange.LOCALHOST;
            }
        }

        return trustedRanges;
    }

    @ConfigValue("http.firewall.proxyIPs")
    private static String proxyIPs;
    private static IPRange.RangeSet proxyRanges;

    /**
     * Returns all proxy IPs as {@link IPRange.RangeSet}
     *
     * @return a range set describing all proxy ranges. This is probably just a single IP addressed, however, using
     *         {@link IPRange.RangeSet} permit to name several, even sub nets. If a request comes from a proxy IP
     *         its X-Forwarded-For header is checked for the original IP.
     */
    protected static IPRange.RangeSet getProxyIPs() {
        if (proxyRanges == null) {
            try {
                proxyRanges = IPRange.paraseRangeSet(proxyIPs);
            } catch (Throwable e) {
                Exceptions.handle()
                          .to(LOG)
                          .error(e)
                          .withSystemErrorMessage("Error parsing config value: 'http.firewall.proxyIPs': %s (%s)")
                          .handle();
                proxyRanges = IPRange.NO_FILTER;
            }
        }

        return proxyRanges;
    }

    @Context
    private GlobalContext ctx;

    private static HttpDataFactory httpDataFactory;
    private ThreadPoolExecutor bossExecutor;
    private ThreadPoolExecutor workerExecutor;
    private ServerBootstrap bootstrap;
    private NioServerSocketChannelFactory channelFactory;
    private HashedWheelTimer timer;

    /*
     * Statistics
     */
    protected static volatile long bytesIn = 0;
    protected static volatile long bytesOut = 0;
    protected static volatile long messagesIn = 0;
    protected static volatile long messagesOut = 0;
    protected static volatile long connections = 0;
    protected static volatile long blocks = 0;
    protected static volatile long requests = 0;
    protected static volatile long chunks = 0;
    protected static volatile long keepalives = 0;
    protected static volatile long idleTimeouts = 0;
    protected static AtomicLong openConnections = new AtomicLong();

    /*
     * Cached values of the last metrics run
     */
    protected static volatile long lastBytesIn = 0;
    protected static volatile long lastBytesOut = 0;
    protected static volatile long lastConnections = 0;
    protected static volatile long lastRequests = 0;

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

    /**
     * Returns the total bytes received so far
     *
     * @return the total bytes received via the http port
     */
    public static long getBytesIn() {
        return bytesIn;
    }

    /**
     * Returns the total bytes sent so far
     *
     * @return the total bytes sent via the http port
     */
    public static long getBytesOut() {
        return bytesOut;
    }

    /**
     * Returns the total messages (packets) sent so far
     *
     * @return the total messages sent via the http port
     */
    public static long getMessagesIn() {
        return messagesIn;
    }

    /**
     * Returns the total messages (packets) received so far
     *
     * @return the total messages received via the http port
     */
    public static long getMessagesOut() {
        return messagesOut;
    }

    /**
     * Returns the total number of connections opened so far
     *
     * @return the total number of connections opened on the http port
     */
    public static long getConnections() {
        return connections;
    }

    /**
     * Returns the total number of connections blocked so far
     *
     * @return the total number of connections blocked via a firewall rule on the http port
     */
    public static long getBlockedConnections() {
        return blocks;
    }

    /**
     * Returns the number of currently open connection
     *
     * @return the number of open connections on the http port
     */
    public static AtomicLong getOpenConnections() {
        return openConnections;
    }

    /**
     * Returns the total number of HTTP requests received by the web server
     *
     * @return the total number of requests received
     */
    public static long getRequests() {
        return requests;
    }

    /**
     * Returns the total number of HTTP chunks received
     *
     * @return the total number of chunks received
     */
    public static long getChunks() {
        return chunks;
    }

    /**
     * Returns the number of keepalives supported
     *
     * @return the number of connections not closed in order to keep them alive.
     */
    public static long getKeepalives() {
        return keepalives;
    }

    /**
     * Returns the number of idle connections killed
     *
     * @return the number of connections closed because they were found to be idle.
     */
    public static long getIdleTimeouts() {
        return idleTimeouts;
    }

    @Override
    public void gather(Collector<Metric> collector) {
        collector.add(differenceMetric("Bytes In", bytesIn, lastBytesIn, "b"));
        lastBytesIn = bytesIn;
        collector.add(differenceMetric("Bytes Out", bytesOut, lastBytesOut, "b"));
        lastBytesOut = bytesOut;
        collector.add(differenceMetric("Connects", connections, lastConnections, null));
        lastConnections = connections;
        collector.add(differenceMetric("Requests", requests, lastRequests, null));
        lastRequests = requests;
        collector.add(new Metric("HTTP",
                                 "Open Connections",
                                 String.valueOf(openConnections.get()),
                                 Metrics.MetricState.GREEN));
    }

    private Metric differenceMetric(String name, long currentValue, long lastValue, String unit) {
        if (lastValue > currentValue) {
            return null;
        }
        return new Metric("HTTP",
                          name,
                          Amount.of((currentValue - lastValue) / 10d).toScientificString(0, unit),
                          Metrics.MetricState.GREEN);
    }
}
