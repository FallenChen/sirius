package sirius.web.controller;

import sirius.kernel.commons.PriorityCollector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 14:22
 * To change this template use File | Settings | File Templates.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Routed {

    int priority() default PriorityCollector.DEFAULT_PRIORITY;

    String value();

    boolean defaultHandler() default false;
}
