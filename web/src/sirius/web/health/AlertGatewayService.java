package sirius.web.health;

import sirius.kernel.di.std.Register;
import sirius.kernel.xml.StructuredOutput;
import sirius.web.services.ServiceCall;
import sirius.web.services.StructuredService;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 29.01.14
 * Time: 15:39
 * To change this template use File | Settings | File Templates.
 */
@Register(name="system/alert-gateway")
public class AlertGatewayService implements StructuredService {
    @Override
    public void call(ServiceCall call, StructuredOutput out) throws Exception {

    }
}
