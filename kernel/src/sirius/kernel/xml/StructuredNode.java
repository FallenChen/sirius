/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.xml;

import sirius.kernel.commons.Value;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

/**
 * Represents a structured node, which is part of a {@link StructuredInput}.
 *
 * @author aha
 */
public interface StructuredNode {
    /**
     * Returns a given node at the relative path.
     */
    StructuredNode queryNode(String xpath) throws XPathExpressionException;

    /**
     * Returns a list of nodes at the relative path.
     */
    List<StructuredNode> queryNodeList(String xpath) throws XPathExpressionException;

    /**
     * Boilerplate for array handling....
     */
    StructuredNode[] queryNodes(String path) throws XPathExpressionException;

    /**
     * Returns a property at the given part.
     */
    String queryString(String path) throws XPathExpressionException;

    /**
     * Queries a {@link sirius.kernel.commons.Value} which provides various conversions.
     */
    Value queryValue(String path) throws XPathExpressionException;

    /**
     * Queries a string via the given XPath. All contained XML is converted to a
     * string.
     */
    String queryXMLString(String path) throws XPathExpressionException;

    /**
     * Checks whether a node or non-empty content is reachable via the given
     * XPath.
     */
    boolean isEmpty(String path) throws XPathExpressionException;

    /**
     * Returns the current node's name.
     */
    String getNodeName();
}
