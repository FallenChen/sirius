package sirius.kernel.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to fill a field from a given config value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface ConfigValue {

    String value();

    boolean required() default false;
}
