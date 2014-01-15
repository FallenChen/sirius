/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import com.google.common.collect.Lists;
import sirius.kernel.commons.Tuple;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 13.01.14
 * Time: 22:20
 * To change this template use File | Settings | File Templates.
 */
public class JoinRecipe {

    private List<Tuple<String, String>> joins = Lists.newArrayList();
    private Property property;

    public JoinRecipe(Property property) {

        this.property = property;
    }

    public List<Tuple<String, String>> getJoins() {
        return joins;
    }

    public Property getProperty() {
        return property;
    }

}
