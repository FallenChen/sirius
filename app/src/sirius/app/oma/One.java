/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import sirius.kernel.commons.Strings;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.01.14
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public class One<E extends Entity> {
    private Long key;
    private E object;
    private Entity owner;
    private String field;
    private Class<E> type;

    protected One(Class<E> type, String field, Entity owner) {
        this.type = type;
        this.field = field;
        this.owner = owner;
    }

    public Long getKey() {
        if (Strings.isFilled(field)) {
            return owner.getId();
        }
        return key;
    }

    public void setKey(Long key) {
        if (Strings.isFilled(field)) {
            throw new UnsupportedOperationException("Cannot set key of remote reference");
        }
        if (!Objects.equals(this.key, key)) {
            this.key = key;
            this.object = null;
        }
    }

    public E getObject() {
        if (field != null) {
            return OMA.select(type).eq(field, owner.getId()).first();
        }
        if (key == null) {
            return null;
        }
        if (object == null) {
            object = OMA.select(type).eq(Entity.ID, owner.getId()).first();
        }
        return object;
    }

    public void setObject(E object) {
        this.object = object;
        if (object == null) {
            key = null;
        } else {
            key = object.getId();
        }
    }
}
