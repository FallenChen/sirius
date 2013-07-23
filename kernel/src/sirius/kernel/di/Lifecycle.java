package sirius.kernel.di;

/**
 * Gets notified when the framework starts up or shuts down.
 */
public interface Lifecycle {
    /**
     * Invoked when the framework starts up.
     */
    void started();

    /**
     * Invoked when the framework shuts down.
     */
    void stopped();

    /**
     * Returns a short name for this lifecycle.
     */
    String getName();
}
