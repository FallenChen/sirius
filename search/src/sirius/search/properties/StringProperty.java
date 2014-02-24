/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search.properties;

import org.elasticsearch.common.xcontent.XContentBuilder;
import sirius.search.annotations.Analyzed;
import sirius.search.annotations.Stored;
import sirius.kernel.di.std.Register;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Contains a string property. If the field wears an {@link Analyzed} annotation, the contents of the field will be
 * analyzed and tokenized by ElasticSearch.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class StringProperty extends Property {

    private final boolean analyzed;

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
        this.analyzed = field.isAnnotationPresent(Analyzed.class);
    }

    @Override
    protected boolean isStored() {
        return field.isAnnotationPresent(Stored.class);
    }

    @Override
    public void createMapping(XContentBuilder builder) throws IOException {
        builder.startObject(getName());
        builder.field("type", getMappingType());
        builder.field("store", isStored() ? "yes" : "no");
        builder.field("index", analyzed ? "analyzed" : "not_analyzed");
        builder.endObject();
    }
}
