/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.commons.Tuple;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance measurement framework which can be used in development as well as in production systems.
 * <p>
 * Can be used to identify bottlenecks or to analyze system performance to measure the average execution time of
 * different tasks.
 * </p>
 * <p>
 * Once the microtiming framework is enabled it will be notified by other frameworks as defined tasks are executed.
 * The execution times are combined and can be queried using {@link sirius.kernel.health.Microtiming#getTimings()}.
 * </p>
 * <p>
 * An example might be an SQL query which is executed in a loop to perform a file import. Since the SQL-Query is
 * always the same (in case or a prepared statement) the execution times will be added to an average which will
 * be stored until the next call to <tt>Microtiming.getTimings()</tt>.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class Microtiming {

    private static volatile boolean enabled = false;
    private static volatile long lastReset;
    private static Map<String, Average> timings = Collections.synchronizedMap(new HashMap<String, Average>());
    private static Map<String, Average> liveSet = Collections.synchronizedMap(new HashMap<String, Average>());

    /**
     * Returns a list of recorded timings.
     * <p>
     * Calling this method will clear the internal <tt>live set</tt>. Therefore, only microtiming key which
     * where submitted since the last call to timing will be returned. Using this approach provides a kind
     * of auto filtering which permits to enable this framework in production systems while still getting
     * reasonable small results.
     * </p>
     *
     * @return all keys along with their average execution time which where submitted since the last call to
     *         <tt>getTimings()</tt>
     */
    public static List<Tuple<String, Average>> getTimings() {
        List<Tuple<String, Average>> result = Tuple.fromMap(liveSet);
        liveSet.clear();
        return result;
    }

    /**
     * Submits a new timing for the given key.
     * <p>
     * Adds the average to the "live set" which will be output on the next call to getTimings()
     * </p>
     * <p>
     * A convenient way to call this method is to use {@link sirius.kernel.commons.Watch#submitMicroTiming(String)}
     * </p>
     *
     * @param key      the key for which the value should be submitted
     * @param duration the number of nanoseconds used as timing for the given key
     */
    public static void submit(String key, long duration) {
        if (!enabled) {
            return;
        }
        Average avg = timings.get(key);
        if (avg == null) {
            avg = new Average();
            timings.put(key, avg);
        }
        avg.addValue(duration / 1000);
        liveSet.put(key, avg);
    }

    /**
     * Checks whether the timing is enabled.
     *
     * @return <tt>true</tt> if the micro timing framework is enabled or <tt>false</tt> otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables/Disables the timing.
     *
     * @param enabled determines whether the framework should be enabled or disabled
     */
    public static void setEnabled(boolean enabled) {
        if (enabled && !Microtiming.enabled) {
            timings.clear();
            liveSet.clear();
            lastReset = System.currentTimeMillis();
        }
        Microtiming.enabled = enabled;
    }

    /**
     * Returns the timestamp of the last reset
     *
     * @return returns the timestamp in milliseconds (as provided by <tt>System.currentTimeMillis()</tt>) since the
     *         last reset.
     */
    public static long getLastReset() {
        return lastReset;
    }

}
