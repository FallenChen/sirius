/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma.query;

import sirius.app.oma.Property;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 13.01.14
 * Time: 22:31
 * To change this template use File | Settings | File Templates.
 */
public class PropertyInfo {

    private String name;
    private Property property;

    public PropertyInfo(String name, Property property) {
        this.name = name;
        this.property = property;
    }

    public String getName() {
        return name;
    }

    public Object transform(Object object) {
        return property.convertToDB(object);
    }


}
