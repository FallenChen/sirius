package sirius.web.services;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 21.11.13
 * Time: 19:37
 * To change this template use File | Settings | File Templates.
 */
public @interface DocNode {

    String title();

    DocProperty[] properties() default {};

    String description() default "";

}
