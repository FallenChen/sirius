/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import com.google.common.collect.Maps;
import sirius.app.oma.annotations.Index;
import sirius.app.oma.annotations.Indices;
import sirius.app.oma.annotations.RefType;
import sirius.app.oma.annotations.Transient;
import sirius.app.oma.schema.ForeignKey;
import sirius.app.oma.schema.Key;
import sirius.app.oma.schema.Table;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.health.Exceptions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.01.14
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class TableDescriptor {

    private final Entity registeredEntity;
    private final Class<?> type;
    private Map<String, Property> properties = Maps.newHashMap();
    private Map<String, JoinRecipe> joinTable = Maps.newTreeMap();
    private String tableName;

    public TableDescriptor(Entity e) {
        this.registeredEntity = e;
        this.type = e.getClass();
        this.tableName = type.getSimpleName().toLowerCase();

        scanFields(type, e);
    }

    private void scanFields(Class<?> type, Entity defaultEntity) {
        for (Field field : type.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Transient.class) && field.getType() != Many.class && !Modifier.isStatic(field.getModifiers())) {
                properties.put(field.getName(), new Property(field, defaultEntity));
            }
        }
        if (type.getSuperclass() != Object.class) {
            scanFields(type.getSuperclass(), defaultEntity);
        }
    }

    public Collection<Property> getProperties() {
        return properties.values();
    }

    public String getTableName() {
        return tableName;
    }

    public <E extends Entity> void updateFetchState(E entity) {
        entity.clearFetchState();
        for (Property p : properties.values()) {
            entity.setFetchValue(p.getName(), p.writeDatabase(entity));
        }
    }

    public Table getTable() {
        Table table = new Table();
        table.setName(getTableName());
        table.getPrimaryKey().add("id");
        for (Property p : getProperties()) {
            table.getColumns().add(p.getColumn());
            if (p.isReference()) {
                ForeignKey fk = new ForeignKey();
                fk.addColumn(1, p.getName());
                fk.addForeignColumn(1, Entity.ID);
                fk.setForeignTable(OMA.getDescriptor(p.getReference()).getTableName());
                fk.setName("fk_" + p.getName() + "_id");
                table.getForeignKeys().add(fk);
            }
        }
        if (type.isAnnotationPresent(Indices.class)) {
            for (Index i : type.getAnnotation(Indices.class).value()) {
                Key key = new Key();
                key.setName(i.name());
                int pos = 1;
                for (String col : i.columns()) {
                    key.addColumn(pos++, col);
                }
                table.getKeys().add(key);
            }
        }
        return table;
    }

    public synchronized JoinRecipe compile(String path) {
        JoinRecipe result = joinTable.get(path);
        if (result != null) {
            return result;
        }

        result = lookup(path);
        joinTable.put(path, result);
        return result;
    }

    private JoinRecipe lookup(String path) {
        if (path.contains(".")) {
            Tuple<String, String> splitted = Strings.split(path, ".");
            Property p = findProperty(splitted.getFirst());
            if (p.getField().getType() != One.class) {
                throw Exceptions.handle()
                                .to(OMA.LOG)
                                .withSystemErrorMessage("%s in %s is not a reference to another table",
                                                        p.getName(),
                                                        type.getName())
                                .handle();
            }
            TableDescriptor subDescriptor = OMA.getDescriptor(p.getField().getAnnotation(RefType.class).value());
            JoinRecipe result = subDescriptor.lookup(splitted.getSecond());
            result.getJoins().add(0, Tuple.create(subDescriptor.getTableName(), p.getName()));

            return result;
        }

        return new JoinRecipe(findProperty(path));
    }

    public Property findProperty(String path) {
        Property p = properties.get(path);
        if (p == null) {
            throw Exceptions.handle()
                            .to(OMA.LOG)
                            .withSystemErrorMessage("Cannot find property %s in %s", path, type.getName())
                            .handle();
        }
        return p;
    }

    @Override
    public String toString() {
        return "TableDescriptor: " + type + " (" + getTableName() + ")";
    }
}
