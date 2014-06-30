/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.properties;

import org.elasticsearch.common.xcontent.XContentBuilder;
import sirius.kernel.di.std.Register;
import sirius.search.annotations.IndexMode;
import sirius.search.annotations.Stored;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Contains a string property. If the field wears an {@link sirius.search.annotations.IndexMode} annotation, the contents of the field will be
 * analyzed and tokenized by ElasticSearch.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class StringProperty extends Property {

    private final String indexMode;

    /**
     * Factory for generating properties based on their field type
     */
    @Register
    public static class Factory implements PropertyFactory {

        @Override
        public boolean accepts(Field field) {
            return String.class.equals(field.getType());
        }

        @Override
        public Property create(Field field) {
            return new StringProperty(field);
        }
    }

    /*
     * Instances are only created by the factory
     */
    private StringProperty(Field field) {
        super(field);
        this.indexMode = field.isAnnotationPresent(IndexMode.class) ? field.getAnnotation(IndexMode.class)
                                                                         .indexMode() : IndexMode.MODE_NOT_ANALYZED;
    }

    @Override
    protected boolean isStored() {
        return field.isAnnotationPresent(Stored.class);
    }

    @Override
    protected boolean isIgnoreFromAll() {
        return false;
    }

    @Override
    public void createMapping(XContentBuilder builder) throws IOException {
        builder.startObject(getName());
        builder.field("type", getMappingType());
        builder.field("store", isStored() ? "yes" : "no");
        builder.field("index", indexMode);
        builder.endObject();
    }
}
