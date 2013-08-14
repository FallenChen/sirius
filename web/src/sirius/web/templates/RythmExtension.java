package sirius.web.templates;

import sirius.kernel.commons.Collector;
import sirius.kernel.commons.Tuple;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface RythmExtension {

    void collectExtensionNames(Collector<Tuple<String, Class<?>>> names);

    void collectExtensionValues(Collector<Tuple<String, Object>> values);

}
