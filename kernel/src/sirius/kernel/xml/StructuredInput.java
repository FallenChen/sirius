/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import javax.xml.xpath.XPathExpressionException;

/**
 * Represents structured data like XML or JSON which was read from a web service
 * call.
 *
 * @author aha
 */
public interface StructuredInput {
    StructuredNode getNode(String xpath) throws XPathExpressionException;
}
