/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.log4j.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.jboss.netty.util.CharsetUtil;
import org.junit.BeforeClass;
import sirius.kernel.commons.Callback;
import sirius.kernel.commons.Value;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.Injector;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.annotations.Parts;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.logging.*;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains global constants which are frequently accessed.
 */
public class Sirius {

    private static final boolean dev;
    private static Config config;
    private static Classpath classpath;
    private static boolean started = false;
    private static boolean initialized = false;
    protected static final Log LOG = Log.get("sirius");

    @Parts(Lifecycle.class)
    private static PartCollection<Lifecycle> lifecycleParticipants;

    static {
        dev = getProperty("debug").asBoolean();
    }

    private static boolean startedAsTest = false;

    /**
     * Determines if the framework is running in development or in production mode.
     *
     * @return <code>true</code> is the framework runs in development mode, false otherwise.
     */
    public static boolean isDev() {
        return startedAsTest || dev;
    }

    /**
     * Determines if the framework is running in development or in production mode.
     *
     * @return <code>true</code> is the framework runs in production mode, false otherwise.
     */
    public static boolean isProd() {
        return !dev;
    }

    private static void setupLogLevels() {
        if (!config.hasPath("health")) {
            return;
        }
        LOG.INFO("Initializing the log system:");
        Config logging = config.getConfig("health");
        for (Map.Entry<String, com.typesafe.config.ConfigValue> entry : logging.entrySet()) {
            LOG.INFO("* Setting %s to: %s", entry.getKey(), logging.getString(entry.getKey()));

            // Setup log4j
            Logger.getLogger(entry.getKey()).setLevel(Level.toLevel(logging.getString(entry.getKey())));

            // Setup java.util.health
            java.util
                    .logging
                    .Logger
                    .getLogger(entry.getKey())
                    .setLevel(convertLog4jLevel(Level.toLevel(logging.getString(entry.getKey()))));
        }
    }


    private static void start() {
        if (started) {
            stop();
        }
        started = true;
        for (Lifecycle lifecycle : lifecycleParticipants.getParts()) {
            LOG.INFO("Starting: %s", lifecycle.getName());
            try {
                lifecycle.started();
            } catch (Throwable e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Startup of: %s failed!", lifecycle.getName())
                          .handle();
            }
        }
    }

    private static void init() {
        initialized = true;
        classpath = new Classpath("component.conf");

        if (startedAsTest) {
            // Load test configurations (will override component configs
            classpath.find(Pattern.compile("component-test\\.conf"), new Callback<Matcher>() {
                @Override
                public void invoke(Matcher value) throws Exception {
                    config = config.withFallback(ConfigFactory.load(value.group()));
                }
            });
        }

        // Apply component configs to core config...
        for (URL url : classpath.getComponentRoots()) {
            config = config.withFallback(ConfigFactory.parseURL(url));
        }

        // Setup log-system based on configuration
        setupLogLevels();

        // Setup native language support
        NLS.init(classpath);

        // Initialize dependency injection...
        Injector.init(new Callback<MutableGlobalContext>() {
            @Override
            public void invoke(MutableGlobalContext value) throws Exception {
                value.registerPart(config, Config.class);
            }
        }, classpath);

        start();
    }

    /**
     * Stops the framework...
     */
    public static void stop() {
        if (!started) {
            return;
        }
        for (Lifecycle lifecycle : lifecycleParticipants.getParts()) {
            LOG.INFO("Stopping: %s", lifecycle.getName());
            try {
                lifecycle.stopped();
            } catch (Throwable e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Stop of: %s failed!", lifecycle.getName())
                          .handle();
            }
        }
        started = false;
    }

    /**
     * If <tt>Sirius</tt> is used as base class for a JUnit test, this method will ensure that the framework is
     * booted into the test mode.
     * <p>
     * It will also make sure, that the framework is only initialized once and not per method / class.
     * </p>
     */
    @BeforeClass
    public static void initializeTestEnvironment() {
        if (!initialized) {
            startedAsTest = true;
            main(null);
        }
    }


    public static void main(String[] args) {
        Watch w = Watch.start();
        if (!getProperty("sirius.manual-health").asBoolean()) {
            setupLogging();
            LOG.INFO(
                    "Updated log4j config! To block this, set the system property 'sirius.manual-health' to true! [-Dsirius.manual-health=true]");
        }
        LOG.INFO("Setting up environment...");
        LOG.INFO("---------------------------------------------------------");
        setupDNSCache();
        setupEncoding();
        LOG.INFO("---------------------------------------------------------");
        LOG.INFO("Loading config");
        LOG.INFO("---------------------------------------------------------");
        setupConfiguration();
        LOG.INFO("---------------------------------------------------------");
        LOG.INFO("Starting sirius");
        LOG.INFO("---------------------------------------------------------");
        init();
        LOG.INFO("---------------------------------------------------------");
        LOG.INFO("System is UP and RUNNING - %s", w.duration());
        LOG.INFO("---------------------------------------------------------");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOG.INFO("Stopping Sirius");
                LOG.INFO("---------------------------------------------------------");
                stop();
            }
        }));
    }

    private static void setupConfiguration() {
        config = ConfigFactory.empty();
        if (Sirius.class.getResource("/application.conf") != null) {
            LOG.INFO("using application.conf from classpath...");
            config = ConfigFactory.load("application.conf").withFallback(config);
        } else {
            LOG.INFO("application.conf not present in classpath");
        }
        if (Sirius.isDev() && new File("develop.conf").exists()) {
            LOG.INFO("using develop.conf from filesystem...");
            config = ConfigFactory.parseFile(new File("develop.conf")).withFallback(config);
        } else if (Sirius.isDev()) {
            LOG.INFO("develop.conf not present work directory");
        }
        if (startedAsTest && Sirius.class.getResource("/test.conf") != null) {
            LOG.INFO("using test.conf from classpath...");
            config = ConfigFactory.parseFile(new File("test.conf")).withFallback(config);
        } else if (Sirius.isDev()) {
            LOG.INFO("develop.conf not present work directory");
        }
        if (new File("instance.conf").exists()) {
            LOG.INFO("using instance.conf from filesystem...");
            config = ConfigFactory.parseFile(new File("instance.conf")).withFallback(config);
        } else {
            LOG.INFO("instance.conf not present work directory");
        }
    }

    /**
     * Initializes log4j as health framework. In development mode, we log everything to the console.
     * In production mode, we use a rolling file appender and log into the logs directory.
     */
    private static void setupLogging() {
        final LoggerRepository repository = Logger.getRootLogger().getLoggerRepository();
        repository.resetConfiguration();
        Logger.getRootLogger().setLevel(Level.INFO);

        if (Sirius.isDev()) {
            ConsoleAppender console = new ConsoleAppender();
            console.setLayout(new PatternLayout("%d{HH:mm:ss.SSS} %-5p [%X{flow}|%t] %c - %m%n"));
            console.setThreshold(Level.DEBUG);
            console.activateOptions();
            Logger.getRootLogger().addAppender(console);
        } else {
            File logsDirectory = new File("logs");
            if (!logsDirectory.exists()) {
                logsDirectory.mkdirs();
            }
            DailyRollingFileAppender fa = new DailyRollingFileAppender();
            fa.setName("FileLogger");
            fa.setFile("logs/application.log");
            fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
            fa.setThreshold(Level.DEBUG);
            fa.setAppend(true);
            fa.activateOptions();
            Logger.getRootLogger().addAppender(fa);
        }

        // Redirect java.utils.health to log4j...
        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        // remove old handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        // add our own
        Handler handler = new Handler() {

            private Formatter formatter = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                repository.getLogger(record.getLoggerName())
                          .log(convertJuliLevel(record.getLevel()),
                               formatter.formatMessage(record),
                               record.getThrown());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        handler.setLevel(java.util.logging.Level.ALL);
        rootLogger.addHandler(handler);
        rootLogger.setLevel(java.util.logging.Level.INFO);
    }

    public static Level convertJuliLevel(java.util.logging.Level juliLevel) {

        if (juliLevel.equals(java.util.logging.Level.FINEST)) {
            return Level.TRACE;
        } else if (juliLevel.equals(java.util.logging.Level.FINER)) {
            return Level.DEBUG;
        } else if (juliLevel.equals(java.util.logging.Level.FINE)) {
            return Level.DEBUG;
        } else if (juliLevel.equals(java.util.logging.Level.INFO)) {
            return Level.INFO;
        } else if (juliLevel.equals(java.util.logging.Level.WARNING)) {
            return Level.WARN;
        } else if (juliLevel.equals(java.util.logging.Level.SEVERE)) {
            return Level.ERROR;
        } else if (juliLevel.equals(java.util.logging.Level.ALL)) {
            return Level.ALL;
        } else if (juliLevel.equals(java.util.logging.Level.OFF)) {
            return Level.OFF;
        }
        return Level.DEBUG;

    }

    public static java.util.logging.Level convertLog4jLevel(Level log4jLevel) {
        if (log4jLevel.equals(Level.TRACE)) {
            return java.util.logging.Level.FINEST;
        } else if (log4jLevel.equals(Level.DEBUG)) {
            return java.util.logging.Level.FINER;
        } else if (log4jLevel.equals(Level.INFO)) {
            return java.util.logging.Level.INFO;
        } else if (log4jLevel.equals(Level.WARN)) {
            return java.util.logging.Level.WARNING;
        } else if (log4jLevel.equals(Level.ERROR)) {
            return java.util.logging.Level.SEVERE;
        } else if (log4jLevel.equals(Level.FATAL)) {
            return java.util.logging.Level.SEVERE;
        } else if (log4jLevel.equals(Level.ALL)) {
            return java.util.logging.Level.ALL;
        } else if (log4jLevel.equals(Level.OFF)) {
            return java.util.logging.Level.OFF;
        }
        return java.util.logging.Level.FINE;
    }

    private static void setupEncoding() {
        LOG.INFO("Setting " + CharsetUtil.UTF_8.name() + " as default encoding (file.encoding)");
        System.setProperty("file.encoding", CharsetUtil.UTF_8.name());
        LOG.INFO("Setting " + CharsetUtil.ISO_8859_1.name() + " as default mime encoding (mail.mime.charset)");
        System.setProperty("mail.mime.charset", CharsetUtil.ISO_8859_1.name());
    }

    private static void setupDNSCache() {
        LOG.INFO("Setting DNS-Cache to 10 seconds...");
        java.security.Security.setProperty("networkaddress.cache.ttl", "10");
    }

    private static Value getProperty(String property) {
        return Value.of(System.getProperty(property));
    }


    public static Config getConfig() {
        return config;
    }

}
