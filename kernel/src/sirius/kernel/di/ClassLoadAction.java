package sirius.kernel.di;

import java.lang.annotation.Annotation;

/**
 * Implementations of this class will be automatically detected and applied to
 * all matching classes on startup.
 */
public interface ClassLoadAction {
    /**
     * Returns the trigger-annotation which is used to identify classes of
     * interest.
     */
    Class<? extends Annotation> getTrigger();

    /**
     * Invoked for each class which contains the trigger-annotation.
     */
    void handle(MutableGlobalContext ctx, Class<?> clazz) throws Exception;
}
