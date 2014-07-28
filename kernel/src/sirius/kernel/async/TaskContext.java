/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;


import sirius.kernel.commons.RateLimit;
import sirius.kernel.commons.Strings;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by aha on 09.07.14.
 */
public class TaskContext {

    private static final String GENERIC = "GENERIC";
    private static final String MDC_SYSTEM = "SYSTEM";
    private TaskContextAdapter adapter;
    private String system = GENERIC;
    private String subSystem = GENERIC;
    private String job = GENERIC;
    private String jobTitle = "";
    private volatile boolean cancelled = false;
    private RateLimit stateUpdate = RateLimit.timeInterval(5, TimeUnit.SECONDS);

    public static TaskContext get() {
        return CallContext.getCurrent().get(TaskContext.class);
    }


    public void log(String message, Object... args) {
        if (adapter != null) {
            adapter.log(Strings.apply(message, args));
        } else {
            Async.LOG.INFO(getSystemString() + ": " + Strings.apply(message, args));
        }
    }

    public void trace(String message, Object... args) {
        if (adapter != null) {
            adapter.trace(Strings.apply(message, args));
        } else {
            Async.LOG.INFO(getSystemString() + ": " + Strings.apply(message, args));
        }
    }

    public void logAsCurrentState(String message, Object... args) {
        log(message, args);
        if (adapter != null) {
            adapter.setState(Strings.apply(message, args));
        }
    }

    public RateLimit shouldUpdateState() {
        return stateUpdate;
    }

    public void inc(String counter, long duration) {
        if (adapter != null) {
            adapter.inc(counter, duration);
        }
    }

    public void markErroneous() {
        if (adapter != null) {
            adapter.markErroneous();
        }
    }

    public boolean isActive() {
        return !cancelled && Async.isRunning();
    }

    public void cancel() {
        cancelled = true;
        if (adapter != null) {
            adapter.cancel();
        }
    }

    public <T> void iterateWhileActive(Iterable<T> iterable, Consumer<T> consumer) {
        for (T obj : iterable) {
            if (!isActive()) {
                return;
            }
            consumer.accept(obj);
        }
    }

    public String getSystemString() {
        return system + "::" + subSystem + "::" + job;
    }

    public String getSystem() {
        return system;
    }

    public TaskContext setSystem(String system) {
        if (Strings.isEmpty(system)) {
            this.system = GENERIC;
        } else {
            this.system = system;
        }
        CallContext.getCurrent().addToMDC(MDC_SYSTEM, getSystemString());
        return this;
    }

    public String getSubSystem() {
        return subSystem;
    }

    public TaskContext setSubSystem(String subSystem) {
        if (Strings.isEmpty(subSystem)) {
            this.subSystem = GENERIC;
        } else {
            this.subSystem = subSystem;
        }
        CallContext.getCurrent().addToMDC(MDC_SYSTEM, getSystemString());
        return this;
    }

    public String getJob() {
        return job;
    }

    public TaskContext setJob(String job) {
        if (Strings.isEmpty(job)) {
            this.job = GENERIC;
        } else {
            this.job = job;
        }
        CallContext.getCurrent().addToMDC(MDC_SYSTEM, getSystemString());
        return this;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public TaskContext setJobTitle(String jobTitle) {
        if (Strings.isEmpty(jobTitle)) {
            this.jobTitle = null;
        } else {
            this.jobTitle = jobTitle;
        }
        return this;
    }

    @Override
    public String toString() {
        if (jobTitle != null) {
            return getSystemString() + ": " + getJobTitle();
        } else {
            return getSystemString();
        }
    }

    public TaskContextAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(TaskContextAdapter adapter) {
        this.adapter = adapter;
    }
}
