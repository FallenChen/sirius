/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml

import sirius.kernel.Sirius
import sirius.kernel.commons.ValueHolder
import sirius.kernel.health.Counter
import sirius.kernel.health.Exceptions
import spock.lang.Specification

import javax.xml.xpath.XPathExpressionException

/**
 * Created by aha on 11.08.14.
 */
class XMLReaderSpec extends Specification {

    void setup() {
        Sirius.initializeTestEnvironment();
    }

    def "XMLReader extracts XPATH expression"() {
        given:
        def check = ValueHolder.of(null);
        def nodes = new Counter();
        def r = new XMLReader();
        and:
        r.addHandler("test", { n ->
            try {
                nodes.inc();
                check.set(n.queryString("value"));
            } catch (XPathExpressionException e) {
                throw Exceptions.handle(e);
            }
        } as NodeHandler);
        when:
        r.parse(new ByteArrayInputStream(
                "<doc><test><value>1</value></test><test><value>2</value></test><test><value>5</value></test></doc>".getBytes()));
        then:
        check.get() == "5"
        and:
        nodes.getCount() == 3
    }
}
