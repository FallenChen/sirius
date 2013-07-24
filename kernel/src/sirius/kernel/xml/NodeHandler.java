package sirius.kernel.xml;

import com.scireum.common.rpc.StructuredNode;

/**
 * Called by the XMLReader for a parsed sub-DOM tree.
 *
 * @author aha
 */
public interface NodeHandler {

    void process(StructuredNode node);
}
