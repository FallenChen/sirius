/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import sirius.app.oma.*;
import sirius.kernel.commons.*;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.web.jdbc.JDBCQuery;
import sirius.web.jdbc.Row;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 11.01.14
 * Time: 20:27
 * To change this template use File | Settings | File Templates.
 */
public class Query<T extends Entity> implements QueryPathCompiler {
    private int entitiesToSkip;
    private int maxEntitiesToReturn;
    private Class<T> table;
    private TableDescriptor descriptor;
    private List<Constraint> constraints;
    private List<Tuple<String, Boolean>> sorting;
    private Map<String, String> joinTable;
    private LinkedHashSet<String> fetchFields;
    private Context parameters;
    private StringBuilder joinPart;
    private String wherePart;
    private String sortPart;

    public Query(Class<T> table) {
        this.table = table;
        this.descriptor = OMA.getDescriptor(table);
    }

    public Query<T> fields(String... fields) {
        fetchFields = Sets.newLinkedHashSet(Arrays.asList(fields));
        fetchFields.add(Entity.ID);
        return this;
    }

    public Query<T> where(Constraint... constraints) {
        if (this.constraints == null) {
            this.constraints = Lists.newArrayList();
        }
        for (Constraint c : constraints) {
            this.constraints.add(c);
        }
        wherePart = null;
        return this;
    }

    public Query<T> eq(String field, Object value) {
        return where(Op.eq(field, value));
    }

    public Query<T> eqIgnoreNull(String field, Object value) {
        return where(Op.eqIgnoreNull(field, value));
    }

    public Query<T> filled(String field) {
        return where(Filled.on(field));
    }

    public Query<T> limit(int entitiesToSkip, int maxEntitiesToReturn) {
        this.entitiesToSkip = Math.abs(entitiesToSkip);
        this.maxEntitiesToReturn = Math.abs(maxEntitiesToReturn);

        return this;
    }

    public Query<T> limit(int maxEntitiesToReturn) {
        this.maxEntitiesToReturn = Math.abs(maxEntitiesToReturn);
        return this;
    }

    public Query<T> start(int entitiesToSkip) {
        this.entitiesToSkip = Math.abs(entitiesToSkip);
        return this;
    }

    public Query<T> orderBy(String field, boolean asc) {
        if (sorting == null) {
            sorting = Lists.newArrayList();
        }
        sorting.add(Tuple.create(field, asc));
        sortPart = null;

        return this;
    }

    public Query<T> orderByAsc(String field) {
        return orderBy(field, true);
    }

    public Query<T> orderByDesc(String field) {
        return orderBy(field, false);
    }

    public T first() {
        List<T> result = limit(0, 1).list();
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public T one() throws ResultNotUniqueException {
        List<T> result = limit(0, 2).list();
        if (result.isEmpty()) {
            return null;
        } else if (result.size() > 1) {
            throw new ResultNotUniqueException();
        } else {
            return result.get(0);
        }
    }

    public int count() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) as NumberOfEntities");
        compileFromJoinWhereOrder(sql);

        try {
            OMA.LOG.FINE(sql);
            JDBCQuery qry = OMA.getDatabase().createQuery(sql.toString());
            if (parameters != null) {
                qry.set(parameters);
            }
            return qry.queryFirst().getValue("NumberOfEntities").asInt(0);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .withSystemErrorMessage("Error executing query: %s - %s (%s)", sql.toString())
                            .to(OMA.LOG)
                            .error(e)
                            .handle();
        }
    }

    public boolean exists() {
        return count() > 0;
    }

    public void perform(final Callback<T> cb) {
        final StringBuilder sql = compileSelect();

        try {
            OMA.LOG.FINE(sql);
            JDBCQuery qry = OMA.getDatabase().createQuery(sql.toString());
            if (parameters != null) {
                qry.set(parameters);
            }
            int maxRows = 0;
            if (maxEntitiesToReturn > 0) {
                maxRows = maxEntitiesToReturn + entitiesToSkip;
            }
            ValueHolder<Integer> skipCount = ValueHolder.of(0);
            final ValueHolder<Integer> finalSkipCount = skipCount;
            qry.perform(new JDBCQuery.RowHandler() {
                @Override
                public boolean handle(Row row) {
                    try {
                        if (finalSkipCount.get() < entitiesToSkip) {
                            finalSkipCount.set(finalSkipCount.get() + 1);
                        } else {
                            T e = table.newInstance();
                            for (Property p : descriptor.getProperties()) {
                                if (fetchFields == null || fetchFields.contains(p)) {
                                    p.readDatabase(e, row.getValue(p.getName()));
                                }
                            }
                            cb.invoke(e);
                        }
                        return true;
                    } catch (Throwable e) {
                        throw Exceptions.handle()
                                        .withSystemErrorMessage("Error executing query: %s - %s (%s)", sql.toString())
                                        .to(OMA.LOG)
                                        .error(e)
                                        .handle();
                    }
                }
            }, maxRows);
        } catch (HandledException e) {
            throw e;
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .withSystemErrorMessage("Error executing query: %s - %s (%s)", sql.toString())
                            .to(OMA.LOG)
                            .error(e)
                            .handle();
        }
    }

    private StringBuilder compileSelect() {
        final StringBuilder sql = new StringBuilder("SELECT ");
        if (fetchFields != null) {
            Monoflop mf = Monoflop.create();
            for (String field : fetchFields) {
                if (mf.successiveCall()) {
                    sql.append(", ");
                }
                sql.append("t.");
                sql.append(field);
            }
        } else {
            sql.append("t.*");
        }
        compileFromJoinWhereOrder(sql);

        return sql;
    }

    private void compileFromJoinWhereOrder(StringBuilder sql) {
        sql.append(" FROM ");
        sql.append(descriptor.getTableName());
        sql.append(" t");
        String where = compileWhere();
        String sort = compileSort();
        if (joinPart != null) {
            sql.append(joinPart);
        }
        if (Strings.isFilled(where)) {
            sql.append(" ");
            sql.append(where);
        }
        if (sort != null) {
            sql.append(sort);
        }
    }

    private String compileSort() {
        if (sorting == null) {
            return null;
        }
        if (sortPart == null) {
            StringBuilder sb = new StringBuilder(" ORDER BY ");
            Monoflop mf = Monoflop.create();
            for (Tuple<String, Boolean> sort : sorting) {
                if (mf.successiveCall()) {
                    sb.append(", ");
                }
                sb.append(compile(sort.getFirst()).getName());
                sb.append(sort.getSecond() ? " ASC" : " DESC");
            }
            sortPart = sb.toString();
        }
        return sortPart;
    }

    public List<T> list() {
        final List<T> result = Lists.newArrayList();
        perform(new Callback<T>() {
            @Override
            public void invoke(T value) throws Exception {
                result.add(value);
            }
        });
        return result;
    }

    public void delete() {
        perform(new Callback<T>() {
            @Override
            public void invoke(T value) throws Exception {
                OMA.delete(value);
            }
        });
    }

    private String compileWhere() {
        if (wherePart == null) {
            StringBuilder result = new StringBuilder();
            int parameterId = 1;
            parameters = Context.create();
            for (Constraint c : constraints) {
                if (c.addsConstraint()) {
                    String condition = c.getSQL(this, PREFIX, String.valueOf(parameterId++), parameters);
                    if (Strings.isFilled(condition)) {
                        if (result.length() == 0) {
                            result.append("WHERE ");
                        } else {
                            result.append("    AND ");
                        }
                        result.append(condition);
                    }
                }
            }
            wherePart = result.toString();
        }
        return wherePart;
    }

    private static final String PREFIX = "t";
    private static final int PREFIX_LENGTH = 2;

    @Override
    public PropertyInfo compile(String path) {
        if (path.startsWith(PREFIX + ".")) {
            path = path.substring(PREFIX_LENGTH);
        }
        JoinRecipe recipe = descriptor.compile(path);
        if (recipe.getJoins().size() > 0) {
            if (joinPart == null) {
                joinPart = new StringBuilder();
            }
            if (joinTable == null) {
                joinTable = Maps.newTreeMap();
            }
        }
        String prefix = PREFIX;
        String joinedPath = PREFIX;
        for (Tuple<String, String> join : recipe.getJoins()) {
            joinedPath += "." + join.getSecond();
            String joinTableName = joinTable.get(joinedPath);
            if (joinTableName == null) {
                joinTableName = "t" + joinTable.size();
                joinPart.append(" LEFT JOIN ")
                        .append(join.getFirst())
                        .append(" ")
                        .append(joinTableName)
                        .append(" ON ")
                        .append(prefix)
                        .append(".")
                        .append(join.getSecond())
                        .append("=")
                        .append(joinTableName)
                        .append(".id");
                joinTable.put(joinedPath, joinTableName);
            }
            prefix = joinTableName;
        }
        return new PropertyInfo(prefix + "." + recipe.getProperty().getName(), recipe.getProperty());
    }

    @Override
    public String toString() {
        return compileSelect().toString();
    }
}
