package sirius.kernel.xml;

/**
 * This signal can be used to interrupt XML processing.
 *
 * @author aha
 */
public interface InterruptSignal {
    boolean isInterrupted();
}
