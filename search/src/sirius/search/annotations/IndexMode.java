/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Changes the index mode of the annotated field.
 * <p>
 * By default, fields are not analysed. However, using this annotation, Elasticsearch can be instructed to apply
 * an analyzer or to not index the field at all (relevant for large fields, not intended to be searched in).
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IndexMode {

    public static final String MODE_ANALYZED = "analyzed";
    public static final String MODE_NOT_ANALYZED = "not_analyzed";
    public static final String MODE_NO = "no";

    String indexMode();
}
