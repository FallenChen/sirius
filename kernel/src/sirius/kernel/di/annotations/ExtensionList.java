package sirius.kernel.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to fill a list of extensions into a given field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface ExtensionList {

    String value();

}
