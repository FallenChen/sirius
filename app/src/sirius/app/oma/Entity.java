/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import com.google.common.collect.Maps;
import sirius.app.oma.annotations.*;
import sirius.app.oma.query.Query;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 11.01.14
 * Time: 20:20
 * To change this template use File | Settings | File Templates.
 */
public abstract class Entity<I> {

    private long id = -1;
    public static final String ID = "id";

    @Transient
    private Map<String, Object> fetchState = Maps.newHashMap();

    public boolean isNew() {
        return id < 1;
    }

    protected final void onBeforeCreate() {
        internalBeforeSaveChecks();
        onBeforeSaveChecks();
        internalSaveChecks();
        onSaveChecks();
    }

    protected void onSaveChecks() {

    }

    private final void internalBeforeSaveChecks() {
        for (Property p : getDescriptor().getProperties()) {
            if (p.hasAnnotation(Trim.class)) {
                Object value = p.read(this);
                if (value != null && value instanceof String) {
                    p.write(this, ((String) value).trim());
                }
            }
            if (p.hasAnnotation(eMailAddress.class)) {
                Object value = p.read(this);
                if (value != null && value instanceof String) {
                    String val = ((String) value).trim();
                    if (Strings.isEmpty(val)) {
                        p.write(this, null);
                    } else {
                        p.write(this, val);
                    }
                }
            }
        }
    }

    private TableDescriptor getDescriptor() {
        return OMA.getDescriptor(getClass());
    }

    private final void internalSaveChecks() {
        for (Property p : getDescriptor().getProperties()) {
            if (p.hasAnnotation(NotNull.class)) {
                if (p.getField().getType() == One.class) {
                    if (((One<?>) p.read(this)).getKey() == null) {
                        throw Exceptions.handle()
                                        .to(OMA.LOG)
                                        .withNLSKey("Entity.fieldMustNotBeNull")
                                        .set("field", p.getLabel())
                                        .handle();
                    }
                } else if (p.read(this) == null) {
                    throw Exceptions.handle()
                                    .to(OMA.LOG)
                                    .withNLSKey("Entity.fieldMustNotBeNull")
                                    .set("field", p.getLabel())
                                    .handle();
                }
            }
            if (p.hasAnnotation(Unique.class)) {
                Query<?> qry = OMA.select(getClass()).eq(p.getName(), p.read(this));
                for (String field : p.getField().getAnnotation(Unique.class).within()) {
                    qry.eq(field, getDescriptor().findProperty(field).read(this));
                }
                List<?> objects = qry.limit(2).list();
                if (objects.size() > 1 || (objects.size() == 1 && this.equals(objects.get(0)))) {
                    throw Exceptions.handle()
                                    .to(OMA.LOG)
                                    .withNLSKey("Entity.fieldNotUnique")
                                    .set("field", p.getLabel())
                                    .set("value", p.read(this))
                                    .handle();
                }
            }

        }
    }

    protected void onBeforeSaveChecks() {

    }

    protected final void onAfterCreate() {

    }

    protected final void onBeforeUpdate() {
        onBeforeSaveChecks();
        internalSaveChecks();
        onSaveChecks();
    }

    protected final void onAfterUpdate() {

    }

    protected final void onBeforeDelete() {
        onBeforeDeleteChecks();
        internalDeleteChecks();
        onDeleteChecks();
    }

    protected void onDeleteChecks() {

    }

    private final void internalDeleteChecks() {
        if (isNew()) {
            return;
        }
        internalDeleteChecks(getClass());
    }

    protected void internalDeleteChecks(Class<?> aClass) {
        if (aClass == null || aClass == Entity.class || aClass == Object.class) {
            return;
        }
        try {
            for (Field field : aClass.getDeclaredFields()) {
                if (field.getType() == Many.class) {
                    Many many = (Many) field.get(this);
                    RefType ref = field.getAnnotation(RefType.class);
                    OnDelete onDelete = field.getAnnotation(OnDelete.class);
                    if (onDelete == null || onDelete.type() == DeleteHandler.RESTRICT && many.query().exists()) {
                        throw Exceptions.createHandled()
                                        .withNLSKey((onDelete != null && Strings.isFilled(onDelete.error())) ? onDelete.error() : "Entity.restrictDelete")
                                        .set("field",
                                             NLS.safeGet(aClass.getSimpleName() + "." + field.getName(),
                                                         aClass.getSimpleName() + ".plural"))
                                        .set("entity", toString())
                                        .handle();
                    } else if (onDelete.type() == DeleteHandler.SET_NULL && many.query().exists()) {
                        OMA.getDatabase()
                           .createQuery("UPDATE " + OMA.getDescriptor(ref.value())
                                                       .getTableName() + " SET " + ref.field() + " = NULL WHERE " + ref.field() + "=${field}")
                           .set("field", getId())
                           .executeUpdate();
                    } else if (onDelete.type() == DeleteHandler.CASCADE) {
                        many.query().delete();
                    }
                    ;
                }
            }
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .error(e)
                            .to(OMA.LOG)
                            .withSystemErrorMessage("Cannot cascade delete of %s: %s (%s)", this)
                            .handle();
        }

    }

    protected void onBeforeDeleteChecks() {

    }

    public void clearFetchState() {
        fetchState.clear();
    }

    public void setFetchValue(String name, Object value) {
        fetchState.put(name, value);
    }

    public Object getFetchValue(String name) {
        return fetchState.get(name);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

}
