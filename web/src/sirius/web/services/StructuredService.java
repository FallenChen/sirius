package sirius.web.services;

import sirius.kernel.xml.StructuredOutput;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 12:03
 * To change this template use File | Settings | File Templates.
 */
public interface StructuredService {
    void call(ServiceCall call, StructuredOutput out) throws Exception;
}
