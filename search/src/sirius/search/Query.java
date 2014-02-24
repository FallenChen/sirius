/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.search;

import com.google.common.collect.Lists;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import sirius.kernel.cache.ValueComputer;
import sirius.kernel.commons.Monoflop;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Microtiming;
import sirius.kernel.nls.NLS;
import sirius.search.constraints.*;
import sirius.search.properties.EnumProperty;
import sirius.search.properties.Property;
import sirius.web.controller.UserContext;
import sirius.web.http.WebContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a query against the database which are created via {@link Index#select(Class)}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/12
 */
public class Query<E extends Entity> {
    private Class<E> clazz;
    private List<Constraint> constraints = Lists.newArrayList();
    private List<Tuple<String, Boolean>> orderBys = Lists.newArrayList();
    private List<Facet> termFacets = Lists.newArrayList();
    private boolean randomize;
    private int start;
    private int limit;
    private String query;
    private int pageSize = 25;
    private boolean primary = false;
    private String index;

    /**
     * Used to create a nwe query for entities of the given class
     *
     * @param clazz the type of entities to query for
     */
    protected Query(Class<E> clazz) {
        this.clazz = clazz;
    }

    /**
     * Adds the given constraints to the query.
     * <p>
     * All constraints are combined together using AND logic, meaning that all constraints need to be satisfied.
     * </p>
     *
     * @param constraints the array of constraints to add
     * @return the query itself for fluent method calls
     */
    public Query<E> where(Constraint... constraints) {
        this.constraints.addAll(Arrays.asList(constraints));
        return this;
    }

    /**
     * Adds the given list of alternative constraints to the query.
     * <p>
     * All constraints are combined together using OR logic, meaning that at least one constraint need to be satisfied.
     * </p>
     *
     * @param constraints the array of constraints to add
     * @return the query itself for fluent method calls
     */
    public Query<E> or(Constraint... constraints) {
        this.constraints.add(Or.on(constraints));
        return this;
    }

    /**
     * Adds an <tt>equals</tt> constraint for the given field and value.
     *
     * @param field the field to check
     * @param value the value to compare against
     * @return the query itself for fluent method calls
     * @see FieldEqual
     */
    public Query<E> eq(String field, Object value) {
        constraints.add(FieldEqual.on(field, value));
        return this;
    }

    /**
     * Adds an <tt>equals</tt> constraint for the given field if the given value is neither <tt>null</tt> nor an
     * empty string.
     *
     * @param field the field to check
     * @param value the value to compare against
     * @return the query itself for fluent method calls
     * @see FieldEqual
     */
    public Query<E> eqIgnoreNull(String field, Object value) {
        if (Strings.isFilled(value)) {
            eq(field, value);
        }
        return this;
    }

    /**
     * Generates a query like "field = v1 OR field = v2..." where v1..vN are values in commaSeparatedValues.
     *
     * @param field                the field to check (should be a regular field, not a list)
     * @param commaSeparatedValues a string of comma separated values like "v1,v2,v3". If the value is empty, no
     *                             constraint is added
     * @return the query itself for fluent method calls
     */
    public Query<E> equalsAny(String field, String commaSeparatedValues) {
        if (Strings.isFilled(commaSeparatedValues)) {
            String[] values = commaSeparatedValues.split(",");
            List<Constraint> constraints = Lists.newArrayList();
            for (String val : values) {
                val = val.trim();
                if (Strings.isFilled(val)) {
                    constraints.add(FieldEqual.on(field, val));
                }
            }
            return or(constraints.toArray(new Constraint[constraints.size()]));
        }
        return this;
    }

    /**
     * Generates a query like "v1 IN field OR v2 IN field" where v1..vN are values in commaSeparatedValues.
     *
     * @param field                the field to check (must be a list field)
     * @param commaSeparatedValues a string of comma separated values like "v1,v2,v3". If the value is empty, no
     *                             constraint is added
     * @return the query itself for fluent method calls
     */
    public Query<E> containsAny(String field, String commaSeparatedValues) {
        if (Strings.isFilled(commaSeparatedValues)) {
            String[] values = commaSeparatedValues.split(",");
            List<Constraint> constraints = Lists.newArrayList();
            for (String val : values) {
                val = val.trim();
                if (Strings.isFilled(val)) {
                    constraints.add(ValueInField.on(val, field));
                }
            }
            return or(constraints.toArray(new Constraint[constraints.size()]));
        }
        return this;
    }

    /**
     * Generates a query like "v1 IN field AND v2 IN field" where v1..vN are values in commaSeparatedValues.
     *
     * @param field                the field to check (must be a list field)
     * @param commaSeparatedValues a string of comma separated values like "v1,v2,v3". If the value is empty, no
     *                             constraint is added
     * @return the query itself for fluent method calls
     */
    public Query<E> containsAll(String field, String commaSeparatedValues) {
        if (Strings.isFilled(commaSeparatedValues)) {
            String[] values = commaSeparatedValues.split(",");
            for (String val : values) {
                val = val.trim();
                if (Strings.isFilled(val)) {
                    where(ValueInField.on(val, field));
                }
            }
        }
        return this;
    }


    /**
     * Adds a <tt>not equal</tt> constraint for the given field and value.
     *
     * @param field the field to check
     * @param value the value to compare against
     * @return the query itself for fluent method calls
     * @see FieldNotEqual
     */
    public Query<E> notEq(String field, Object value) {
        constraints.add(FieldNotEqual.on(field, value));
        return this;
    }

    /**
     * Adds a <tt>filled</tt> constraint for the given field and value.
     *
     * @param field the field to check
     * @return the query itself for fluent method calls
     * @see Filled
     */
    public Query<E> filled(String field) {
        constraints.add(Filled.on(field));
        return this;
    }

    /**
     * Adds an <tt>in</tt> constraint for the given field and value.
     * <p>This requires the field to have at least the given value in it (might have others as well).</p>
     *
     * @param field the field to check
     * @param value the value to compare against
     * @return the query itself for fluent method calls
     * @see ValueInField
     */
    public Query<E> in(String field, Object value) {
        constraints.add(ValueInField.on(value, field));
        return this;
    }

    /**
     * Adds a textual query across all searchable fields.
     *
     * @param query the query to search for. Does actually support the complete Lucene query syntax like
     *              <code>field:value OR field2:value1</code>
     * @return the query itself for fluent method calls
     */
    public Query<E> query(String query) {
        if (Strings.isFilled(query)) {
            this.query = query;
            where(QueryString.query(query));
        }

        return this;
    }

    /**
     * Sets the index to use.
     * <p>
     * For "normal" entities, the index is automatically computed but can be overridden by this method.
     * </p>
     *
     * @param indexToUse the name of the index to use. The index prefix used by this system will be automatically
     *                   added
     * @return the query itself for fluent method calls
     */
    public Query<E> index(String indexToUse) {
        this.index = Index.getIndexName(indexToUse);

        return this;
    }

    /**
     * Adds an order by clause for the given field in ascending order.
     *
     * @param field the field to order by
     * @return the query itself for fluent method calls
     */
    public Query<E> orderByAsc(String field) {
        orderBys.add(new Tuple<String, Boolean>(field, true));
        return this;
    }

    /**
     * Adds an order by clause for the given field in descending order.
     *
     * @param field the field to order by
     * @return the query itself for fluent method calls
     */
    public Query<E> orderByDesc(String field) {
        orderBys.add(new Tuple<String, Boolean>(field, false));
        return this;
    }

    /**
     * Picks random items from the result set instead of the first N.
     *
     * @return the query itself for fluent method calls
     */
    public Query<E> randomize() {
        randomize = true;
        return this;
    }

    /**
     * Simple translator which uses the key as parameter for {@link NLS#get(String)}
     */
    public static final ValueComputer<String, String> NLS_TRANSLATOR = new ValueComputer<String, String>() {
        @Nullable
        @Override
        public String compute(@Nonnull String key) {
            return NLS.get(key);
        }
    };

    /**
     * Adds a term facet to be filled by the query.
     *
     * @param field      the field to scan
     * @param value      the value to filter by
     * @param translator the translator used to turn field values into visible filter values
     * @return the query itself for fluent method calls
     */
    public Query<E> addTermFacet(String field, String value, ValueComputer<String, String> translator) {
        final Property p = Index.getDescriptor(clazz).getProperty(field);
        if (p instanceof EnumProperty && translator == null) {
            translator = new ValueComputer<String, String>() {
                @Nullable
                @Override
                public String compute(@Nonnull String value) {
                    return ((EnumProperty) p).transformFromSource(value).toString();
                }
            };
        }
        termFacets.add(new Facet(NLS.get(clazz.getSimpleName() + "." + field), field, value, translator));
        if (Strings.isFilled(value)) {
            where(FieldEqual.on(field, value).asFilter());
        }

        return this;
    }

    /**
     * Adds a term facet to be filled by the query. Loads the filter value directly from the given http request.
     *
     * @param field      the field to scan
     * @param request    the request to read the current filter value from
     * @param translator the translator used to turn field values into visible filter values
     * @return the query itself for fluent method calls
     */
    public Query<E> addTermFacet(String field, WebContext request, ValueComputer<String, String> translator) {
        addTermFacet(field, request.get(field).getString(), translator);
        return this;
    }

    /**
     * Adds a term facet to be filled by the query.
     *
     * @param field the field to scan
     * @param value the value to filter by
     * @return the query itself for fluent method calls
     */
    public Query<E> addTermFacet(String field, String value) {
        addTermFacet(field, value, null);
        return this;
    }

    /**
     * Adds a term facet to be filled by the query. Loads the filter value directly from the given http request.
     *
     * @param field   the field to scan
     * @param request the request to read the current filter value from
     * @return the query itself for fluent method calls
     */
    public Query<E> addTermFacet(String field, WebContext request) {
        addTermFacet(field, request.get(field).getString());
        return this;
    }

    /**
     * Sets the firsts index of the requested result slice.
     *
     * @param start the zero based index of the first requested item from within the result
     * @return the query itself for fluent method calls
     */
    public Query<E> start(int start) {
        this.start = start;
        return this;
    }

    /**
     * Sets the max. number of items to return.
     *
     * @param limit the max. number of items to return
     * @return the query itself for fluent method calls
     */
    public Query<E> limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Combines {@link #start(int)} and {@link #limit(int)} in one call.
     *
     * @param start the zero based index of the first requested item from within the result
     * @param limit the max. number of items to return
     * @return the query itself for fluent method calls
     */
    public Query<E> limit(int start, int limit) {
        return start(start).limit(limit);
    }

    /**
     * Setups start and limiting so that the result begins at the given start and contains at most one page
     * (defined by <tt>pageSize</tt>) items.
     *
     * @param start the one based index of the first item to return
     * @return the query itself for fluent method calls
     */
    public Query<E> page(int start) {
        int effectiveStart = Math.max(0, start - 1);
        return limit(effectiveStart, pageSize);
    }

    /**
     * Forces the framework to load the entities from their primary shard.
     * <p>
     * This can be used to minimize the risk of optimistic lock errors
     * </p>
     *
     * @return the query itself for fluent method calls
     */
    public Query<E> fromPrimary() {
        this.primary = true;
        return this;
    }

    /**
     * Executes the query and returns the first matching entity.
     *
     * @return the first matching entity or <tt>null</tt> if no entity was found
     */
    @Nullable
    public E queryFirst() {
        try {
            SearchRequestBuilder srb = buildSearch();
            if (Index.LOG.isFINE()) {
                Index.LOG
                     .FINE("SEARCH-FIRST: %s.%s: %s",
                           Index.getIndex(clazz),
                           Index.getDescriptor(clazz).getType(),
                           buildQuery());
            }
            return transformFirst(srb);
        } catch (Throwable e) {
            throw Exceptions.handle(Index.LOG, e);
        }
    }

    private SearchRequestBuilder buildSearch() {
        SearchRequestBuilder srb = Index.getClient()
                                        .prepareSearch(index != null ? index : Index.getIndex(clazz))
                                        .setTypes(Index.getDescriptor(clazz).getType());
        if (primary) {
            srb.setPreference("_primary");
        }
        if (randomize) {
            srb.addSort(SortBuilders.scriptSort("Math.random()", "number").order(SortOrder.ASC));

        } else {
            for (Tuple<String, Boolean> sort : orderBys) {
                srb.addSort(sort.getFirst(), sort.getSecond() ? SortOrder.ASC : SortOrder.DESC);
            }
        }
        for (Facet field : termFacets) {
            srb.addFacet(FacetBuilders.termsFacet(field.getName()).field(field.getName()));
        }
        QueryBuilder qb = buildQuery();
        if (qb != null) {
            srb.setQuery(qb);
        }
        FilterBuilder sb = buildFilter();
        if (sb != null) {
            if (qb == null && termFacets.isEmpty()) {
                srb.setPostFilter(sb);
            } else {
                srb.setQuery(QueryBuilders.filteredQuery(qb == null ? QueryBuilders.matchAllQuery() : qb, sb));
            }
        }
        if (start > 0) {
            srb.setFrom(start);
        }
        if (limit > 0) {
            srb.setSize(limit);
        }
        srb.setVersion(true);

        return srb;
    }

    /**
     * Executes the query and returns a list of all matching entities.
     *
     * @return the list of matching entities. If no entities match, an empty list will be returned
     */
    @Nonnull
    public List<E> queryList() {
        return queryResultList().getResults();
    }

    /**
     * Executes the query and returns a list of all matching entities as well as all filter facets
     * in form of a {@link ResultList}
     *
     * @return all matching entities along with the collected facet filters
     */
    @Nonnull
    public ResultList<E> queryResultList() {
        try {
            SearchRequestBuilder srb = buildSearch();
            if (Index.LOG.isFINE()) {
                Index.LOG
                     .FINE("SEARCH: %s.%s: %s",
                           Index.getIndex(clazz),
                           Index.getDescriptor(clazz).getType(),
                           buildQuery());
            }
            return transform(srb);
        } catch (Throwable e) {
            throw Exceptions.handle(Index.LOG, e);
        }
    }

    /**
     * Executes the query and counts the number of matching entities.
     *
     * @return the number of matching entities
     */
    public long count() {
        try {
            CountRequestBuilder crb = Index.getClient()
                                           .prepareCount(index != null ? index : Index.getIndex(clazz))
                                           .setTypes(Index.getDescriptor(clazz).getType());
            QueryBuilder qb = buildQuery();
            if (qb != null) {
                crb.setQuery(qb);
            }
            FilterBuilder sb = buildFilter();
            if (sb != null) {
                crb.setQuery(QueryBuilders.filteredQuery(qb, sb));
            }
            if (Index.LOG.isFINE()) {
                Index.LOG
                     .FINE("COUNT: %s.%s: %s",
                           Index.getIndex(clazz),
                           Index.getDescriptor(clazz).getType(),
                           buildQuery());
            }
            return transformCount(crb);
        } catch (Throwable t) {
            throw Exceptions.handle(Index.LOG, t);
        }
    }

    /**
     * Executes the query and checks if at least one entity matches.
     *
     * @return <tt>true</tt> if at least one entity is matched by the query, <tt>false</tt> otherwise
     */
    public boolean exists() {
        return count() > 0;
    }

    private QueryBuilder buildQuery() {
        List<QueryBuilder> queries = new ArrayList<QueryBuilder>();
        for (Constraint constraint : constraints) {
            QueryBuilder qb = constraint.createQuery();
            if (qb != null) {
                queries.add(qb);
            }
        }
        if (queries.isEmpty()) {
            return null;
        } else if (queries.size() == 1) {
            return queries.get(0);
        } else {
            BoolQueryBuilder result = QueryBuilders.boolQuery();
            for (QueryBuilder qb : queries) {
                result.must(qb);
            }
            return result;
        }
    }

    private FilterBuilder buildFilter() {
        List<FilterBuilder> filters = new ArrayList<FilterBuilder>();
        for (Constraint constraint : constraints) {
            FilterBuilder sb = constraint.createFilter();
            if (sb != null) {
                filters.add(sb);
            }
        }
        if (filters.isEmpty()) {
            return null;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            BoolFilterBuilder result = FilterBuilders.boolFilter();
            for (FilterBuilder qb : filters) {
                result.must(qb);
            }
            return result;
        }
    }

    /**
     * Internal execution of the query along with the transformation of the result for a list
     *
     * @param builder the completed query
     * @return the result of the query
     * @throws Exception in case on an error when executing the query
     */
    protected ResultList<E> transform(SearchRequestBuilder builder) throws Exception {
        Watch w = Watch.start();
        SearchResponse searchResponse = builder.execute().actionGet();
        ResultList<E> result = new ResultList<E>(termFacets, searchResponse);
        EntityDescriptor descriptor = Index.getDescriptor(clazz);
        for (SearchHit hit : searchResponse.getHits()) {
            E entity = clazz.newInstance();
            entity.initSourceTracing();
            entity.setId(hit.getId());
            entity.setVersion(hit.getVersion());
            descriptor.readSource(entity, hit.getSource());
            result.getResults().add(entity);
        }
        if (Index.LOG.isFINE()) {
            Index.LOG
                 .FINE("SEARCH: %s.%s: SUCCESS: %d - %d ms",
                       Index.getIndex(clazz),
                       Index.getDescriptor(clazz).getType(),
                       searchResponse.getHits().totalHits(),
                       searchResponse.getTookInMillis());
        }
        if (Microtiming.isEnabled()) {
            w.submitMicroTiming("LIST: " + toString(true));
        }
        return result;
    }

    /**
     * Internal execution of the query along with the transformation of the result for a single entity
     *
     * @param builder the completed query
     * @return the result of the query
     * @throws Exception in case on an error when executing the query
     */
    protected E transformFirst(SearchRequestBuilder builder) throws Exception {
        Watch w = Watch.start();
        SearchResponse searchResponse = builder.execute().actionGet();
        E result = null;
        if (searchResponse.getHits().hits().length > 0) {
            SearchHit hit = searchResponse.getHits().hits()[0];
            result = clazz.newInstance();
            result.initSourceTracing();
            result.setId(hit.getId());
            result.setVersion(hit.getVersion());
            Index.getDescriptor(clazz).readSource(result, hit.getSource());
        }
        if (Index.LOG.isFINE()) {
            Index.LOG
                 .FINE("SEARCH-FIRST: %s.%s: SUCCESS: %d - %d ms",
                       Index.getIndex(clazz),
                       Index.getDescriptor(clazz).getType(),
                       searchResponse.getHits().totalHits(),
                       searchResponse.getTookInMillis());
        }
        if (Microtiming.isEnabled()) {
            w.submitMicroTiming("FIRST: " + toString(true));
        }
        return result;
    }

    /**
     * Internal execution of the query along with the transformation of the result for a count of entities
     *
     * @param builder the completed query
     * @return the result of the query
     * @throws Exception in case on an error when executing the query
     */
    protected long transformCount(CountRequestBuilder builder) {
        Watch w = Watch.start();
        CountResponse res = builder.execute().actionGet();
        if (Index.LOG.isFINE()) {
            Index.LOG
                 .FINE("COUNT: %s.%s: SUCCESS: %d",
                       Index.getIndex(clazz),
                       Index.getDescriptor(clazz).getType(),
                       res.getCount());
        }
        if (Microtiming.isEnabled()) {
            w.submitMicroTiming("COUNT: " + toString(true));
        }
        return res.getCount();
    }

    /**
     * Executes the query and returns the resulting items as a {@link Page}.
     *
     * @return the result of the query along with all facets and paging-metadata
     */
    public Page<E> queryPage() {
        Watch w = Watch.start();
        int originalLimit = limit;
        limit++;
        ResultList<E> result = new ResultList<>(termFacets, null);
        try {
            result = queryResultList();
        } catch (Exception e) {
            UserContext.handle(e);
        }
        boolean hasMore = false;
        if (result.size() > originalLimit) {
            hasMore = true;
            result.getResults().remove(result.size() - 1);
        }
        return new Page<>(query, start + 1, start + result.size(), result, hasMore, w.duration(), pageSize);
    }

    /**
     * Executes the result and calls the given <tt>handler</tt> for each item in the result.
     * <p>
     * This is intended to be used to process large result sets as these are automatically scrolls through.
     * </p>
     *
     * @param handler the handler used to process each result item
     */
    public void iterate(ResultHandler<E> handler) {
        try {
            SearchRequestBuilder srb = buildSearch();
            srb.setSearchType(SearchType.SCAN);
            srb.setSize(10); // 10 per shard!
            srb.setScroll(org.elasticsearch.common.unit.TimeValue.timeValueSeconds(60));
            EntityDescriptor entityDescriptor = Index.getDescriptor(clazz);
            if (Index.LOG.isFINE()) {
                Index.LOG.FINE("ITERATE: %s.%s: %s", Index.getIndex(clazz), entityDescriptor.getType(), buildQuery());
            }

            SearchResponse searchResponse = srb.execute().actionGet();

            while (true) {
                Watch w = Watch.start();
                searchResponse = Index.getClient()
                                      .prepareSearchScroll(searchResponse.getScrollId())
                                      .setScroll(org.elasticsearch.common.unit.TimeValue.timeValueSeconds(60))
                                      .execute()
                                      .actionGet();

                if (Index.LOG.isFINE()) {
                    Index.LOG
                         .FINE("SEARCH-SCROLL: %s.%s: SUCCESS: %d - %d ms",
                               Index.getIndex(clazz),
                               entityDescriptor.getType(),
                               searchResponse.getHits().totalHits(),
                               searchResponse.getTookInMillis());
                }
                if (Microtiming.isEnabled()) {
                    w.submitMicroTiming("SCROLL: " + toString(true));
                }

                for (SearchHit hit : searchResponse.getHits()) {
                    E entity = clazz.newInstance();
                    entity.setId(hit.getId());
                    entity.initSourceTracing();
                    entity.setVersion(hit.getVersion());
                    entityDescriptor.readSource(entity, hit.getSource());

                    try {
                        if (!handler.handleRow(entity)) {
                            return;
                        }
                    } catch (Exception e) {
                        Exceptions.createHandled().to(Index.LOG).error(e).handle();
                    }

                }
                //Break condition: No hits are returned
                if (searchResponse.getHits().hits().length == 0) {
                    break;
                }
            }
        } catch (Throwable t) {
            throw Exceptions.handle(Index.LOG, t);
        }
    }

    /**
     * Computes a string representation of the given query.
     *
     * @param skipConstraintValues determines if filter values should be added or be replaced with placeholders.
     * @return a string representation of the this query
     */
    public String toString(boolean skipConstraintValues) {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(clazz.getName());
        if (!constraints.isEmpty()) {
            sb.append(" WHERE ");
            Monoflop mf = Monoflop.create();
            for (Constraint constraint : constraints) {
                if (mf.successiveCall()) {
                    sb.append(" AND ");
                }
                sb.append(constraint.toString(skipConstraintValues));
            }
        }
        if (randomize) {
            sb.append(" RANDOMIZED");
        } else if (!orderBys.isEmpty()) {
            sb.append(" ORDER BY");
            for (Tuple<String, Boolean> orderBy : orderBys) {
                sb.append(" ");
                sb.append(orderBy.getFirst());
                sb.append(orderBy.getSecond() ? " ASC" : " DESC");
            }
        }
        if (start > 0 || limit > 0) {
            sb.append(" LIMIT ");
            sb.append(skipConstraintValues ? "?" : start);
            sb.append(", ");
            sb.append(skipConstraintValues ? "?" : limit);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Deletes all entities which are matched by this query.
     */
    public void delete() {
        try {
            Watch w = Watch.start();
            DeleteByQueryRequestBuilder builder = Index.getClient()
                                                       .prepareDeleteByQuery(index != null ? index : Index.getIndex(
                                                               clazz))
                                                       .setTypes(Index.getDescriptor(clazz).getType());
            QueryBuilder qb = buildQuery();
            if (qb != null) {
                builder.setQuery(qb);
            }
            if (Index.LOG.isFINE()) {
                Index.LOG
                     .FINE("DELETE: %s.%s: %s",
                           Index.getIndex(clazz),
                           Index.getDescriptor(clazz).getType(),
                           buildQuery());
            }
            builder.execute().actionGet();
            if (Microtiming.isEnabled()) {
                w.submitMicroTiming("DELETE: " + toString(true));
            }
        } catch (Throwable e) {
            throw Exceptions.handle(Index.LOG, e);
        }
    }

}
