/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import sirius.app.oma.query.Query;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.01.14
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public class Many<E extends Entity> {
    private E object;
    private Entity owner;
    private Class<E> type;
    private String field;

    protected Many(Class<E> type, String field, Entity owner) {
        this.type = type;
        this.field = field;
    }

    public Query<E> query() {
        return OMA.select(type).eq(field, owner);
    }

}
