package sirius.kernel.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be applied to private fields. Once the containing object
 * is passed to {@link ComponentContext#wire(Object)} the field is filled with
 * the instance of {@link ComponentContext}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Context {
}
