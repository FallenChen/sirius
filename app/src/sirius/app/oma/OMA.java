
/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.app.oma.query.Query;
import sirius.app.oma.schema.MySQLDatabaseDialect;
import sirius.app.oma.schema.SchemaTool;
import sirius.app.oma.schema.SchemaUpdateAction;
import sirius.app.oma.schema.Table;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Monoflop;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.jdbc.Database;
import sirius.web.jdbc.Row;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 11.01.14
 * Time: 20:20
 * To change this template use File | Settings | File Templates.
 */
public class OMA {

    public static final Log LOG = Log.get("oma");
    private static Map<Class<?>, TableDescriptor> descriptorMap;

    public static <E extends Entity> E create(E entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("entity");
            }
            if (!entity.isNew()) {
                throw Exceptions.handle()
                                .withSystemErrorMessage(
                                        "The given entity %s of type %s already exists an cannot be created",
                                        entity,
                                        entity.getClass())
                                .to(LOG)
                                .handle();
            }
            entity.onBeforeCreate();
            TableDescriptor td = getDescriptor(entity.getClass());
            Context ctx = Context.create();
            for (Property p : td.getProperties()) {
                if (!p.isId()) {
                    ctx.put(p.getName(), p.writeDatabase(entity));
                }
            }
            if (OMA.LOG.isFINE()) {
                OMA.LOG.FINE("INSERT: " + td.getTableName() + " - " + ctx);
            }
            Row generatedKeys = getDatabase().insertRow(td.getTableName(), ctx);
            entity.setId(generatedKeys.getValue("GENERATED_KEY").asLong(entity.getId()));

            entity.onAfterCreate();
            td.updateFetchState(entity);

            return entity;
        } catch (SQLException e) {
            throw Exceptions.handle()
                            .error(e)
                            .to(LOG)
                            .withSystemErrorMessage("A database error occurred while creating '%s': %s", entity)
                            .handle();
        }
    }

    public static <T extends Entity> Query<T> retrieve(Class<T> table) {
        return new Query(table);
    }

    public static <E extends Entity> E update(E entity) {
        try {
            if (entity == null) {
                throw new IllegalArgumentException("entity");
            }
            if (entity.isNew()) {
                return create(entity);
            }
            TableDescriptor td = getDescriptor(entity.getClass());
            entity.onBeforeUpdate();

            Context ctx = Context.create();
            StringBuilder sb = new StringBuilder("UPDATE ");
            sb.append(td.getTableName());
            sb.append(" SET ");
            Monoflop mf = Monoflop.create();
            for (Property p : td.getProperties()) {
                if (!p.isId()) {
                    if (!Objects.equals(entity.getFetchValue(p.getName()), p.writeDatabase(entity))) {
                        if (mf.successiveCall()) {
                            sb.append(", ");
                        }
                        sb.append(p.getName());
                        sb.append(" = ${");
                        sb.append(p.getName());
                        sb.append("}");
                        ctx.put(p.getName(), p.writeDatabase(entity));
                    }
                }
            }
            if (mf.firstCall()) {
                return entity;
            }
            sb.append(" WHERE id = ${id}");
            ctx.put("id", entity.getId());
            OMA.LOG.FINE(sb);
            getDatabase().createQuery(sb.toString()).set(ctx).executeUpdate();

            entity.onAfterUpdate();
            td.updateFetchState(entity);

            return entity;
        } catch (SQLException e) {
            throw Exceptions.handle()
                            .error(e)
                            .to(LOG)
                            .withSystemErrorMessage("A database error occurred while updating '%s': %s", entity)
                            .handle();
        }
    }

    public static void delete(Entity entity) {
        try {
            if (entity.isNew()) {
                return;
            }
            TableDescriptor td = getDescriptor(entity.getClass());
            entity.onBeforeDelete();

            Context ctx = Context.create();
            StringBuilder sql = new StringBuilder("DELETE FROM ");
            sql.append(td.getTableName());
            sql.append(" WHERE id = ${id}");
            ctx.put("id", entity.getId());
            OMA.LOG.FINE(sql);
            getDatabase().createQuery(sql.toString()).set(ctx).executeUpdate();
        } catch (SQLException e) {
            throw Exceptions.handle()
                            .error(e)
                            .to(LOG)
                            .withSystemErrorMessage("A database error occurred while deleting '%s': %s", entity)
                            .handle();
        }
    }

    @sirius.kernel.di.std.Context
    private static GlobalContext ctx;

    public static TableDescriptor getDescriptor(Class<?> aClass) {
        TableDescriptor td = getDescriptorMap().get(aClass);
        if (td == null) {
            throw Exceptions.handle().withSystemErrorMessage("Unknown entity type: %s", aClass).to(LOG).handle();
        }

        return td;
    }

    private static Map<Class<?>, TableDescriptor> getDescriptorMap() {
        if (descriptorMap == null) {
            Map<Class<?>, TableDescriptor> newMap = Maps.newHashMap();
            for (Entity e : ctx.getParts(Entity.class)) {
                newMap.put(e.getClass(), new TableDescriptor(e));
            }

            descriptorMap = newMap;
        }

        return descriptorMap;
    }

    public static Collection<TableDescriptor> getDescriptors() {
        return Collections.unmodifiableCollection(getDescriptorMap().values());
    }

    @ConfigValue("jdbc.oma.database")
    private static String connection;

    private static Database database;

    public static Database getDatabase() {
        if (database == null) {
            database = Database.get(connection);
        }

        return database;
    }

    @ConfigValue("jdbc.oma.syncSchemaOnStartup")
    private static boolean syncSchemaOnStartup;

    @Register
    public static class CRUDLifecycle implements Lifecycle {

        @Override
        public void started() {
            if (syncSchemaOnStartup) {
                for (SchemaUpdateAction sua : migrateSchema(false)) {
                    if (!sua.isDataLossPossible()) {
                        LOG.INFO("Executing schema change: %s", sua.getReason());
                        for (String qry : sua.getSql()) {
                            try {
                                getDatabase().createQuery(qry).executeUpdate();
                            } catch (SQLException e) {
                                LOG.WARN("Schema change failed: %s", e.getMessage());
                            }
                        }
                    } else {
                        LOG.WARN("Skipping schema change due to possible data loss: %s", sua.getReason());
                    }
                }
            }
        }

        @Override
        public void stopped() {

        }

        @Override
        public String getName() {
            return "oma";
        }
    }

    public static List<SchemaUpdateAction> migrateSchema(boolean dropTables) {
        try {
            List<Table> targetSchema = Lists.newArrayList();
            for (TableDescriptor td : OMA.getDescriptors()) {
                targetSchema.add(td.getTable());
            }
            Connection c = getDatabase().getConnection();
            try {
                return new SchemaTool("", new MySQLDatabaseDialect()).migrateSchemaTo(c, targetSchema, dropTables);
            } finally {
                c.close();
            }
        } catch (SQLException e) {
            throw Exceptions.handle()
                            .error(e)
                            .to(LOG)
                            .withSystemErrorMessage("Error while detecting schema changes: %s")
                            .handle();
        }
    }

}
