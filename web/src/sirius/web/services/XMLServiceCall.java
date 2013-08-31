package sirius.web.services;

import com.google.common.base.Charsets;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.xml.StructuredOutput;
import sirius.kernel.xml.XMLStructuredOutput;
import sirius.web.http.WebContext;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 12:24
 * To change this template use File | Settings | File Templates.
 */
public class XMLServiceCall extends ServiceCall {
    public XMLServiceCall(WebContext ctx) {
        super(ctx);
    }

    @Override
    protected StructuredOutput createOutput() {
        return new XMLStructuredOutput(ctx.respondWith()
                                          .outputStream(HttpResponseStatus.OK,
                                                        "text/xml;charset=" + Charsets.UTF_8.name(),
                                                        null));
    }
}
