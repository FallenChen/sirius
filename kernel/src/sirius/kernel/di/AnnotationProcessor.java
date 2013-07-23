package sirius.kernel.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Implementations of this class will be automatically detected and used to wire objects (to handle annotations placed on fields).
 */
public interface AnnotationProcessor {
    /**
     * Returns the trigger-annotation which is used to identify fields of
     * interest.
     */
    Class<? extends Annotation> getTrigger();

    /**
     * Invoked for each field which contains the trigger-annotation.
     */
    void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception;
}
