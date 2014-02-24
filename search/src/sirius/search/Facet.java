/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search;

import com.google.common.collect.Lists;
import org.elasticsearch.search.facet.terms.TermsFacet;
import sirius.kernel.cache.ValueComputer;
import sirius.kernel.commons.Strings;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a filter facet used by search / query results.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class Facet {
    protected Page parent;
    private String name;
    private String title;
    private String value;
    private final ValueComputer<String, String> translator;
    private List<FacetItem> items = Lists.newArrayList();

    /**
     * Creates a new faced with the given parameters.
     *
     * @param title      the visible name of the facet
     * @param field      the internal name of the facet
     * @param value      the selected value
     * @param translator the translator which provides "official" labels for filter values.
     */
    public Facet(String title,
                 String field,
                 @Nullable String value,
                 @Nullable ValueComputer<String, String> translator) {
        this.name = field;
        this.title = title;
        this.value = value;
        this.translator = translator;
    }

    /**
     * Returns all items collected for this facet.
     *
     * @return a list of all items of this facet
     */
    public List<FacetItem> getItems() {
        return items;
    }

    /**
     * Creates a query string which can be appended to a link in order to toggle the given item.
     *
     * @param item the item to toggle
     * @return a query string (?x=y..) used to toggle the given facet item
     */
    public String createToggleQueryString(FacetItem item) {
        return parent.createQueryString(name, item.isActive() ? "" : item.getKey(), true);
    }

    /**
     * Returns the name of this facet.
     *
     * @return the internal name of this facet
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the title of this fact.
     *
     * @return the visible title of this facet
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the selected value.
     *
     * @return the selected filter value of this facet
     */
    public String getValue() {
        return value;
    }

    /**
     * Loads all items from the given {@link TermsFacet}
     *
     * @param facet the facet to load the data from
     */
    public void loadFrom(TermsFacet facet) {
        for (TermsFacet.Entry entry : facet.getEntries()) {
            String key = entry.getTerm().string();
            items.add(new FacetItem(key,
                                    translator == null ? key : translator.compute(key),
                                    entry.getCount(),
                                    Strings.areEqual(value, key)));
        }
    }

    /**
     * Adds a facet item
     *
     * @param key   the filter value of the item
     * @param title the public visible name of the item
     * @param count the number of matched for this item
     */
    public void addItem(String key, String title, long count) {
        items.add(new FacetItem(key,
                                Strings.isFilled(title) ? title : translator.compute(key),
                                count,
                                Strings.areEqual(value, key)));
    }

    /**
     * Determines if this facet has at least one item
     *
     * @return <tt>true</tt> if this facet has at least one item, <tt>false</tt> otherwise
     */
    public boolean hasItems() {
        return !items.isEmpty();
    }
}
