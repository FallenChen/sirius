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
 * <p>
 * This is basically a XML node which can be queried using xpath.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface StructuredNode {
    /**
     * Returns a given node at the relative path.
     *
     * @param xpath the xpath used to retrieve the resulting node
     * @return the node returned by the given xpath expression
     * @throws XPathExpressionException if an invalid xpath was given
     */
    StructuredNode queryNode(String xpath) throws XPathExpressionException;

    /**
     * Returns a list of nodes at the relative path.
     *
     * @param xpath the xpath used to retrieve the resulting nodes
     * @return the list of nodes returned by the given xpath expression
     * @throws XPathExpressionException if an invalid xpath was given
     */
    List<StructuredNode> queryNodeList(String xpath) throws XPathExpressionException;

    /**
     * Same as queryNodeList, boilerplate for array handling.
     *
     * @param path the xpath used to retrieve the resulting nodes
     * @return the array of nodes returned by the given xpath expression
     * @throws XPathExpressionException if an invalid xpath was given
     */
    StructuredNode[] queryNodes(String path) throws XPathExpressionException;

    /**
     * Returns the property at the given relative path as string.
     *
     * @param path the xpath used to retrieve property
     * @return a string representation of the value returned by the given xpath expression
     * @throws XPathExpressionException if an invalid xpath was given
     */
    String queryString(String path) throws XPathExpressionException;

    /**
     * Queries a {@link sirius.kernel.commons.Value} by evaluating the given xpath.
     *
     * @param path the xpath used to retrieve property
     * @return a Value wrappping the value returned by the given xpath expression
     * @throws XPathExpressionException if an invalid xpath was given
     */
    Value queryValue(String path) throws XPathExpressionException;

    /**
     * Queries a string via the given XPath. All contained XML is converted to a
     * string.
     *
     * @param path the xpath used to retrieve the xml sub tree
     * @return a string representing the xml sub-tree returned by the given xpath expression
     * @throws XPathExpressionException if an invalid xpath was given
     */
    String queryXMLString(String path) throws XPathExpressionException;

    /**
     * Checks whether a node or non-empty content is reachable via the given
     * XPath.
     *
     * @param path the xpath to be checked
     * @return <tt>true</tt> if a node or non empty property was found, <tt>false</tt> otherwise
     * @throws XPathExpressionException if an invalid xpath was given
     */
    boolean isEmpty(String path) throws XPathExpressionException;

    /**
     * Returns the current nodes name.
     *
     * @return returns the name of the node represented by this object
     */
    String getNodeName();
}
