/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;


import com.google.common.collect.Lists;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;
import sirius.kernel.nls.NLS;

import java.util.Collections;
import java.util.List;

/**
 * The logging facade used by the system.
 * <p>
 * <b>Note:</b> Instead of "just" logging exceptions, handle them with {@link sirius.kernel.health.Exceptions#handle()}
 * to generate sophisticated error messages and to permit other parts of the framework to intercept error
 * handling.
 * </p>
 * <p>
 * In contrast to other approaches, it is not recommended to create a logger per class, but rather one per
 * framework or sub system. It should have a concise name, all lowercase without any dots. The log level of each
 * logger is read from the configuration using <code>logging.[NAME]</code>. It may be set to one of:
 * <ul>
 * <li>DEBUG</li>
 * <li>INFO</li>
 * <li>WARN</li>
 * <li>ERROR</li>
 * </ul>
 * </p>
 * <p>
 * Internally uses log4j to perform all logging operations. Still it is recommended to only log through this facade
 * and not to rely on any log4j specific behaviour.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Log {

    private final Logger logger;
    private static final List<Log> all = Lists.newCopyOnWriteArrayList();

    @Parts(LogTap.class)
    private static PartCollection<LogTap> taps;

    /**
     * Generates a new logger with the given name
     * <p>
     * The given name should be short and simple. It is not recommended to create a logger per class but one for
     * each framework or subsystem.
     * </p>
     *
     * @param name the name of the logger. This should be a simple name, completely lowercase, without any dots
     * @return a new logger logging with the given name.
     */
    public static Log get(String name) {
        Log result = new Log(Logger.getLogger(name));
        all.add(result);
        return result;
    }

    /**
     * Returns a list of all known loggers.
     *
     * @return a list of all known loggers
     */
    public static List<Log> getAllLoggers() {
        return Collections.unmodifiableList(all);
    }

    /**
     * Helper method the change the log-level for a given logger.
     * <p>
     * By default the system configuration is used to set the leg level (logging.[name]=LEVEL). Also the "logger"
     * command in the console can be used to change the log level at runtime.
     * </p>
     *
     * @param logger the name of the logger to change
     * @param level  the desired log level
     */
    public static void setLevel(String logger, Level level) {
        // Setup log4j
        Logger.getLogger(logger).setLevel(level);

        // Setup java.util.logging
        java.util.logging.Logger.getLogger(logger).setLevel(convertLog4jLevel(level));
    }

    /**
     * Converts a given java.util.logging.Level to a log4j level.
     *
     * @param juliLevel the java.util.logging level
     * @return the converted equivalent for log4j
     */
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

    /**
     * Converts a given log4j to a java.util.logging.Level level.
     *
     * @param log4jLevel the log4j level
     * @return the converted equivalent java.util.logging
     */
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

    /*
     * Use get(String) to create a new instance
     */
    private Log(Logger logger) {
        super();
        this.logger = logger;
    }

    /**
     * Logs the given message at INFO level
     * <p>
     * The given object is converted to a string if necessary. The INFO level should be used for informative
     * messages to the system operator which occur at a low rate
     * </p>
     *
     * @param msg the message to be logged
     */
    public void INFO(Object msg) {
        if (msg == null) {
            return;
        }
        if (logger.isInfoEnabled()) {
            fixMDC();
            if (msg instanceof Throwable) {
                logger.info(((Throwable) msg).getMessage(), (Throwable) msg);
            } else {
                logger.info(msg.toString());
            }
        }
        tap(msg, logger.isInfoEnabled(), Level.INFO);
    }

    /**
     * Used to log the given message <tt>msg</tt> at <b>INFO</b> level if debug mode is enabled
     * ({@link sirius.kernel.Sirius#isDev()}). Otherwise the message will be logged as <b>FINE</b>.
     *
     * @param msg the message to log
     */
    public void DEBUG_INFO(Object msg) {
        if (Sirius.isDev()) {
            INFO(msg);
        } else {
            FINE(msg);
        }
    }

    /*
     * Used to cut endless loops while feeding taps
     */
    private static ThreadLocal<Boolean> frozen = new ThreadLocal<Boolean>();

    /*
     * Notify all log taps
     */
    private void tap(Object msg, boolean wouldLog, Level level) {
        if (Boolean.TRUE.equals(frozen.get())) {
            return;
        }
        try {
            frozen.set(Boolean.TRUE);
            if (taps != null) {
                for (LogTap tap : taps) {
                    tap.handleLogMessage(new LogMessage(NLS.toUserString(msg),
                                                        level,
                                                        this,
                                                        wouldLog,
                                                        Thread.currentThread().getName()));
                }
            }
        } finally {
            frozen.set(Boolean.FALSE);
        }
    }

    /*
     * Transfers our MDC to the one used by log4j
     */
    private void fixMDC() {
        CallContext.getCurrent().applyToLog4j();
    }

    /**
     * Formats the given message at the INFO level using the supplied parameters.
     * <p>
     * The INFO level should be used for informative messages to the system operator which occur at a low rate
     * </p>
     *
     * @param msg    the message containing placeholders as understood by {@link Strings#apply(String, Object...)}
     * @param params the parameters used to format the resulting log message
     */
    public void INFO(String msg, Object... params) {
        msg = Strings.apply(msg, params);
        if (logger.isInfoEnabled()) {
            fixMDC();
            logger.info(msg);
        }
        tap(msg, logger.isInfoEnabled(), Level.INFO);
    }

    /**
     * Logs the given message at the FINE level
     * <p>
     * The given object is converted to a string if necessary. The FINE level can be used for in depth debug or trace
     * messages used when developing a system. Sill the rate should be kept bearable to enable this level in
     * production systems to narrow down errors.
     * </p>
     *
     * @param msg the message to be logged
     */
    public void FINE(Object msg) {
        if (logger.isDebugEnabled()) {
            fixMDC();
            if (msg instanceof Throwable) {
                logger.debug(((Throwable) msg).getMessage(), (Throwable) msg);
            } else {
                logger.debug(NLS.toUserString(msg));
            }
        }
        tap(msg, logger.isDebugEnabled(), Level.DEBUG);
    }

    /**
     * Formats the given message at the FINE level using the supplied parameters.
     * <p>
     * The FINE level can be used for in depth debug or trace messages used when developing a system.
     * Sill the rate should be kept bearable to enable this level in production systems to narrow down errors.
     * </p>
     *
     * @param msg    the message containing placeholders as understood by {@link Strings#apply(String, Object...)}
     * @param params the parameters used to format the resulting log message
     */
    public void FINE(String msg, Object... params) {
        msg = Strings.apply(msg, params);
        if (logger.isDebugEnabled()) {
            fixMDC();
            logger.debug(msg);
        }
        tap(msg, logger.isDebugEnabled(), Level.DEBUG);
    }

    /**
     * Logs the given message at the WARN level
     * <p>
     * The given object is converted to a string if necessary. The WARN level can be used to signal unexpected
     * situations which do not (yet) result in an error or problem.
     * </p>
     *
     * @param msg the message to be logged
     */
    public void WARN(Object msg) {
        fixMDC();
        if (msg instanceof Throwable) {
            logger.warn(((Throwable) msg).getMessage(), (Throwable) msg);
        } else {
            logger.warn(NLS.toUserString(msg));
        }
        tap(msg, true, Level.WARN);
    }

    /**
     * Formats the given message at the WARN level using the supplied parameters.
     * <p>
     * The WARN level can be used to signal unexpected situations which do not (yet) result in an error or problem.
     * </p>
     *
     * @param msg    the message containing placeholders as understood by {@link Strings#apply(String, Object...)}
     * @param params the parameters used to format the resulting log message
     */
    public void WARN(String msg, Object... params) {
        msg = Strings.apply(msg, params);

        fixMDC();
        logger.warn(msg);
        tap(msg, true, Level.WARN);
    }

    /**
     * Logs the given message at the SEVERE or ERROR level
     * <p>
     * The given object is converted to a string if necessary. The ERROR level can be used to signal problems or error
     * which occurred in the system. It is recommended to handle exceptions using {@link Exceptions} - which will
     * eventually also call this method, but provides sophisticated error handling.
     * </p>
     *
     * @param msg the message to be logged
     */
    public void SEVERE(Object msg) {
        fixMDC();
        if (msg instanceof Throwable) {
            logger.error(((Throwable) msg).getMessage(), (Throwable) msg);
        } else {
            logger.error(NLS.toUserString(msg));
        }
        tap(msg, true, Level.ERROR);
    }

    /**
     * Determines if FINE message will be logged.
     * <p>
     * This can be used to decide whether "expensive" log messages should be constructed at all. Using
     * {@link #FINE(String, Object...)} doesn't require this check since the message is only formatted if it will be
     * logged. However, if the computation of one of the parameters is complex, one might sill want to surround the
     * log message by an appropriate if statement calling this method.
     * </p>
     *
     * @return <tt>true</tt> if this logger logs FINE message, <tt>false</tt> otherwise
     */
    public boolean isFINE() {
        return logger.isDebugEnabled();
    }

    /**
     * Returns the name of this logger
     *
     * @return the name supplied by {@link #get(String)}.
     */
    public String getName() {
        return logger.getName();
    }
}
