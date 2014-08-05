/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import org.junit.Test;
import sirius.kernel.Sirius;
import sirius.kernel.commons.ValueHolder;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class TestReader extends Sirius {

    @Test
    public void testReader() throws Exception {
        ValueHolder<String> check = ValueHolder.of(null);
        Counter nodes = new Counter();
        XMLReader r = new XMLReader();
        r.addHandler("test", n -> {
            try {
                nodes.inc();
                check.set(n.queryString("value"));
            } catch (XPathExpressionException e) {
                throw Exceptions.handle(e);
            }
        });
        r.parse(new ByteArrayInputStream(
                "<doc><test><value>1</value></test><test><value>2</value></test><test><value>3</value></test></doc>".getBytes()));
        assertEquals("3", check.get());
        assertEquals(3l, nodes.getCount());
    }

}
