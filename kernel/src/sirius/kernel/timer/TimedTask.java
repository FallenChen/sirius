package sirius.kernel.timer;

public interface TimedTask {

    /**
     * Called every time the timer interval is fired.
     */
    void runTimer() throws Exception;

}