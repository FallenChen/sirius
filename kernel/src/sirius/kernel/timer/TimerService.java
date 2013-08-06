package sirius.kernel.timer;

import sirius.kernel.async.Async;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.annotations.Parts;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Internal service which is responsible for executing timers.
 */
@Register(name = "timer", classes = {TimerService.class, Lifecycle.class})
public class TimerService implements Lifecycle {

    protected static final Log LOG = Log.get("timer");
    private static final String TIMER = "timer";

    @Parts(EveryMinute.class)
    private PartCollection<EveryMinute> everyMinute;
    private long lastOneMinuteExecution = 0;

    @Parts(EveryTenMinutes.class)
    private PartCollection<EveryTenMinutes> everyTenMinutes;
    private long lastTenMinutesExecution = 0;

    @Parts(EveryHour.class)
    private PartCollection<EveryHour> everyHour;
    private long lastHourExecution = 0;

    private Timer timer;
    private ReentrantLock timerLock = new ReentrantLock();

    private class InnerTimerTask extends TimerTask {

        @Override
        public void run() {
            try {
                runOneMinuteTimers();
                if (TimeUnit.MINUTES
                            .convert(System.currentTimeMillis() - lastTenMinutesExecution,
                                     TimeUnit.MILLISECONDS) >= 10) {
                    runTenMinuteTimers();
                }
                if (TimeUnit.MINUTES
                            .convert(System.currentTimeMillis() - lastHourExecution, TimeUnit.MILLISECONDS) >= 60) {
                    runOneHourTimers();
                }
            } catch (Throwable t) {
                Exceptions.handle(LOG, t);
            }
        }

    }

    /**
     * Returns the timestamp of the last execution of the one minute timer.
     */
    public String getLastOneMinuteExecution() {
        if (lastOneMinuteExecution == 0) {
            return "-";
        }
        return NLS.toUserString(new Date(lastOneMinuteExecution), true);
    }

    public String getLastTenMinutesExecution() {
        if (lastTenMinutesExecution == 0) {
            return "-";
        }
        return NLS.toUserString(new Date(lastTenMinutesExecution), true);
    }

    public String getLastHourExecution() {
        if (lastHourExecution == 0) {
            return "-";
        }
        return NLS.toUserString(new Date(lastHourExecution), true);
    }


    @Override
    public void started() {
        try {
            timerLock.lock();
            try {
                if (timer == null) {
                    timer = new Timer(true);
                } else {
                    timer.cancel();
                    timer = new Timer(true);
                }
                timer.schedule(new InnerTimerTask(), 1000 * 60, 1000 * 60);
            } finally {
                timerLock.unlock();
            }
        } catch (Throwable t) {
            Exceptions.handle(LOG, t);
        }
    }

    @Override
    public void stopped() {
        try {
            timerLock.lock();
            try {
                if (timer != null) {
                    timer.cancel();
                }
            } finally {
                timerLock.unlock();
            }
        } catch (Throwable t) {
            Exceptions.handle(LOG, t);
        }
    }

    @Override
    public String getName() {
        return "timer (System Timer Services)";
    }

    protected void runOneMinuteTimers() {
        for (final TimedTask task : everyMinute.getParts()) {
            executeTask(task);
        }
        lastOneMinuteExecution = System.currentTimeMillis();
    }

    private void executeTask(final TimedTask task) {
        Async.executor(TIMER).start(new Runnable() {
            @Override
            public void run() {
                try {
                    task.runTimer();
                } catch (Throwable t) {
                    Exceptions.handle(LOG, t);
                }
            }
        }).dropOnOverload(new Runnable() {
            @Override
            public void run() {
                LOG.INFO("Dropping timer tasks due to system overload!");
            }
        }).execute();
    }

    protected void runTenMinuteTimers() {
        for (final TimedTask task : everyTenMinutes.getParts()) {
            executeTask(task);
        }
        lastTenMinutesExecution = System.currentTimeMillis();
    }

    protected void runOneHourTimers() {
        for (final TimedTask task : everyHour.getParts()) {
            executeTask(task);
        }
        lastHourExecution = System.currentTimeMillis();
    }

}
