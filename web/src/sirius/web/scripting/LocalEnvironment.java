/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.scripting;

import com.google.common.collect.Lists;
import org.apache.log4j.Level;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.LogMessage;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import java.time.Instant;
import java.util.List;

/**
 * Created by aha on 15.06.14.
 */
public class LocalEnvironment {
    private String path;
    private Instant loadTime;
    private List<LogMessage> messages = Lists.newArrayList();
    private ScriptContext ctx = new SimpleScriptContext();

    public LocalEnvironment(String path) {
        this.path = path;
        this.loadTime = Instant.now();
        put("env", this);
    }

    public LocalEnvironment put(String key, Object value) {
        ctx.setAttribute(key, value, ScriptContext.ENGINE_SCOPE);
        return this;
    }

    public ScriptContext getContext() {
        return ctx;
    }

    public void logException(Throwable t) {
        HandledException h = null;
        if (t instanceof HandledException) {
            h = (HandledException) t;
        } else {
            h = Exceptions.handle(ServerScriptEngine.LOG, t);
        }
        log(h.getMessage(), Level.FATAL, false);
    }

    private void log(String message, Level level, boolean forwardTologger) {
        synchronized (messages) {
            messages.add(new LogMessage(message, level, null, true, Thread.currentThread().getName()));
            while (messages.size() > 100) {
                messages.remove(0);
            }
        }
        if (forwardTologger) {
            if (level == Level.DEBUG) {
                ServerScriptEngine.LOG.FINE(path + ": " + message);
            } else if (level == Level.INFO) {
                ServerScriptEngine.LOG.INFO(path + ": " + message);
            } else if (level == Level.WARN) {
                ServerScriptEngine.LOG.WARN(path + ": " + message);
            } else {
                ServerScriptEngine.LOG.SEVERE(path + ": " + message);
            }
        }
    }

    public void info(String message) {
        log(message, Level.INFO, false);
    }

    public void debug(String message) {
        log(message, Level.DEBUG, false);
    }

    public void warn(String message) {
        log(message, Level.WARN, false);
    }

    public void error(String message) {
        log(message, Level.FATAL, false);
    }
}
