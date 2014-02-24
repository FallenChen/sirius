/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search;

import sirius.kernel.commons.Monoflop;
import sirius.kernel.commons.Strings;
import sirius.kernel.nls.NLS;

import java.util.List;

/**
 * Represents a slice of a result set which is being "paged through".
 * <p>
 * Pages are mainly created by {@link sirius.search.Query#queryPage()}.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class Page<E> {

    private String query;
    private final int start;
    private final int end;
    private final ResultList<E> items;
    private final boolean more;
    private final String duration;
    private int pageSize;
    private List<Facet> facets;
    private boolean hasFacets;

    /**
     * Creates a new page based on the given parameters.
     *
     * @param query    the query string which was used to create the result set
     * @param start    the index of the first item on this page
     * @param end      the index of the last item on this page
     * @param items    the list of items
     * @param more     determines if there are more items in the result set
     * @param duration the time it took to execute the query
     * @param pageSize the maximal number of items on a page
     */
    public Page(String query, int start, int end, ResultList<E> items, boolean more, String duration, int pageSize) {
        this.query = query;
        this.start = start;
        this.end = end;
        this.items = items;
        this.more = more;
        this.duration = duration;
        this.pageSize = pageSize;
    }

    /**
     * Returns the query string which was used to create the result set.
     *
     * @return the query string which was used to create the result set
     */
    public String getQuery() {
        return query == null ? "" : query;
    }

    /**
     * Returns the absolute index (1 based) of the first item on this page.
     *
     * @return the one based index of the first item on this page
     */
    public int getStart() {
        return start;
    }

    /**
     * Returns the absolute index (1 based) of the last item on this page.
     *
     * @return the one based index of the last item on this page
     */
    public int getEnd() {
        return end;
    }

    /**
     * Returns the items on this page.
     *
     * @return the list of items on this page
     */
    public List<E> getItems() {
        return items.getResults();
    }

    /**
     * Returns the index of the previous page.
     *
     * @return the index of the first item of the previous page
     */
    public int getPreviousStart() {
        return Math.max(1, start - pageSize);
    }

    /**
     * Returns the index of the next page.
     *
     * @return the index of the first item of the next page
     */
    public int getNextStart() {
        return start + pageSize;
    }

    /**
     * Determines if there is a previous page.
     *
     * @return <tt>true</tt> if there is a page before this one
     */
    public boolean hasLess() {
        return start > 1;
    }

    /**
     * Determines if there is a next page.
     *
     * @return <tt>true</tt> if there are more items in the underlying result
     */
    public boolean hasMore() {
        return more;
    }

    /**
     * Returns a string representation naming the first and last index contained on this page.
     *
     * @return a string naming the first and last index of this page
     */
    public String getRange() {
        if (end == 0) {
            return NLS.get("Page.noResults");
        }
        return start + " - " + end;
    }

    /**
     * Returns the search duration.
     *
     * @return a string representation of the search duration used to create the underlying result set
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Creates a query string containing all filters, the search query and a <tt>start</tt> parameter selecting the
     * previous page.
     *
     * @return a query string selecting the previous page
     */
    public String createPrevPageQueryString() {
        return createQueryString("start", String.valueOf(getPreviousStart()), false);
    }

    /**
     * Creates a query string containing all filters, the search query and a <tt>start</tt> parameter selecting the
     * next page.
     *
     * @return a query string selecting the next page
     */
    public String createNextPageQueryString() {
        return createQueryString("start", String.valueOf(getNextStart()), false);
    }

    /**
     * Creates a query string containing all filters and the search query and a <tt>start</tt> parameter selecting the
     * current page.
     *
     * @return a query string selecting the current page
     */
    public String createQueryString() {
        return createQueryString(null, null, false);
    }

    /**
     * Creates a query string containing all filters and the search query along with a given custom field.
     *
     * @param field      the additional field to set
     * @param value      the value of the field to set
     * @param resetStart determines if the start value should be kept (<tt>false</tt>) or resetted to 1 (<tt>true</tt>)
     * @return a query string containing all filters, the query and the given parameter
     */
    public String createQueryString(String field, String value, boolean resetStart) {
        StringBuilder sb = new StringBuilder();
        boolean fieldFound = false;
        Monoflop mf = Monoflop.create();
        for (Facet f : getFacets()) {
            if (Strings.areEqual(field, f.getName())) {
                fieldFound = true;
                if (Strings.isFilled(value)) {
                    sb.append(mf.firstCall() ? "" : "&");
                    sb.append(field);
                    sb.append("=");
                    sb.append(Strings.urlEncode(value));
                }
            } else if (Strings.isFilled(f.getValue())) {
                sb.append(mf.firstCall() ? "" : "&");
                sb.append(f.getName());
                sb.append("=");
                sb.append(Strings.urlEncode(f.getValue()));
            }
        }
        if (!resetStart) {
            sb.append(mf.firstCall() ? "" : "&");
            sb.append("start=");
            if ("start".equals(field)) {
                fieldFound = true;
                sb.append(value);
            } else {
                sb.append(start);
            }
        }
        if ("query".equals(field)) {
            sb.append(mf.firstCall() ? "" : "&");
            sb.append("query=");
            fieldFound = true;
            sb.append(Strings.urlEncode(value));
        } else if (Strings.isFilled(query)) {
            sb.append(mf.firstCall() ? "" : "&");
            sb.append("query=");
            sb.append(Strings.urlEncode(query));
        }
        if (!fieldFound && Strings.isFilled(value)) {
            sb.append(mf.firstCall() ? "" : "&");
            sb.append(field);
            sb.append("=");
            sb.append(Strings.urlEncode(value));
        }
        return sb.toString();
    }

    /**
     * Adds a filter facet to this result page.
     *
     * @param facet the facet to add
     */
    public void addFacet(Facet facet) {
        getFacets().add(facet);
        facet.parent = this;
        hasFacets = true;
    }

    /**
     * Returns all filter facets available.
     *
     * @return all filter facets of the underlying result set
     */
    public List<Facet> getFacets() {
        if (facets == null) {
            facets = items.getFacets();
            for (Facet facet : facets) {
                facet.parent = this;
                if (facet.hasItems()) {
                    hasFacets = true;
                }
            }
        }

        return facets;
    }

    /**
     * Determines if there is at least one filter facet with filter items.
     *
     * @return <tt>true</tt> if there is at least one filter facet with items
     */
    public boolean hasFacets() {
        if (facets == null) {
            getFacets();
        }

        return hasFacets;
    }
}
