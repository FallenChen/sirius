package sirius.web.services;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 21.11.13
 * Time: 19:37
 * To change this template use File | Settings | File Templates.
 */
public @interface DocProperty {

    static final String TYPE_STRING = "string";
    static final String TYPE_INT = "int";
    static final String TYPE_BOOL = "boolean";
    static final String TYPE_ARRAY = "array of ";
    static final String TYPE_NODE = "node: ";

    String title();

    String type() default TYPE_STRING;

    String description() default "";

}
