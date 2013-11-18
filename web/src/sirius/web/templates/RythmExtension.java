/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.templates;

import sirius.kernel.commons.Collector;
import sirius.kernel.commons.Tuple;

/**
 * Defines an extension which provides auto-declared variables to Rythm templates.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public interface RythmExtension {

    /**
     * Enumerates all variables paired with their according type.
     * <p>
     * This method is used at compile-time, to perform type checks.
     * </p>
     *
     * @param names a collector which can be used to supply variables along with their type.
     */
    void collectExtensionNames(Collector<Tuple<String, Class<?>>> names);

    /**
     * Enumerates all variables paired with their according value.
     * <p>
     * This method is used at runtime, to supply the appropriate values.
     * </p>
     *
     * @param values a collector which can be used to supply variables along with their value.
     */
    void collectExtensionValues(Collector<Tuple<String, Object>> values);

}
