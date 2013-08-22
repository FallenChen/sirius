package sirius.web.services;

import com.google.common.base.Charsets;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.commons.Strings;
import sirius.kernel.xml.StructuredOutput;
import sirius.web.http.WebContext;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 12:24
 * To change this template use File | Settings | File Templates.
 */
public class JSONServiceCall extends ServiceCall {
    public JSONServiceCall(String[] subURI, WebContext ctx) {
        super(subURI, ctx);
    }


    @Override
    protected StructuredOutput createOutput() {
        return ctx.respondWith().json();
    }
}
