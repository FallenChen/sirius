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
* Time: 22:32
* To change this template use File | Settings | File Templates.
*/
public interface QueryPathCompiler {

    QueryPathCompiler IDENTITY = new QueryPathCompiler() {
        @Override
        public PropertyInfo compile(String path) {
            return new PropertyInfo(path, Property.EMPTY);
        }
    };

    PropertyInfo compile(String path);

}
