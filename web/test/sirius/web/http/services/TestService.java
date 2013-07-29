package sirius.web.http.services;

import sirius.kernel.di.annotations.Register;
import sirius.kernel.xml.StructuredOutput;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 13:29
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "test")
public class TestService implements StructuredService {
    @Override
    public void call(ServiceCall call, StructuredOutput out) {
        out.beginResult();
        out.property("is", "ok");
        out.endResult();
    }
}
