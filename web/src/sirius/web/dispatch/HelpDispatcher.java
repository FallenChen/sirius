package sirius.web.dispatch;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.di.annotations.ConfigValue;
import sirius.kernel.di.annotations.Register;
import sirius.web.WebContext;
import sirius.web.WebDispatcher;

import java.io.File;
import java.net.URL;

@Register
public class HelpDispatcher implements WebDispatcher {

    @Override
    public int getPriority() {
        return 30;
    }

    @ConfigValue("help.indexTemplate")
    private String indexTemplate;

    @Override
    public boolean dispatch(WebContext ctx) throws Exception {
        if (!ctx.getRequest().getUri().startsWith("/help") || HttpMethod.GET != ctx.getRequest().getMethod()) {
            return false;
        }
        String uri = ctx.getRequestedURI();
        if ("/help".equals(uri)) {
            uri = "/help/" + indexTemplate;
        }
        if (uri.contains(".") && !uri.endsWith("html")) {
            // Dispatch static content...
            URL url = getClass().getResource(uri);
            if (url == null) {
                ctx.respond().error(HttpResponseStatus.NOT_FOUND);
            } else if ("file".equals(url.getProtocol())) {
                ctx.respond().file(new File(url.toURI()));
            } else {
                ctx.respond().resource(url.openConnection());
            }
        } else {
            // Render help template...
            ctx.respond().cache().nlsTemplate(uri);
        }
        return true;
    }

}
