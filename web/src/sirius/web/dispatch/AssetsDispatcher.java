/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.dispatch;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.serversass.Generator;
import org.serversass.Output;
import sirius.kernel.Sirius;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;
import sirius.web.http.WebServer;
import sirius.web.templates.Content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Dispatches all URLs below <code>/assets</code>.
 * <p>
 * All assets are fetched from the classpath and should be located in the <tt>resources</tt> source root (below the
 * <tt>assets</tt> directory).
 * </p>
 * <p>
 * This dispatcher tries to support caching as well as zero-copy delivery of static files if possible.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Register
public class AssetsDispatcher implements WebDispatcher {

    @Override
    public int getPriority() {
        return PriorityCollector.DEFAULT_PRIORITY;
    }

    @Override
    public boolean preDispatch(WebContext ctx) throws Exception {
        return false;
    }

    @ConfigValue("http.generated-directory")
    private String cacheDir;
    private File cacheDirFile;

    @Override
    public boolean dispatch(WebContext ctx) throws Exception {
        if (!ctx.getRequest().getUri().startsWith("/assets") || HttpMethod.GET != ctx.getRequest().getMethod()) {
            return false;
        }
        String uri = ctx.getRequestedURI();
        if (uri.startsWith("/assets/dynamic")) {
            uri = uri.substring(16);
            Tuple<String, String> pair = Strings.split(uri, "/");
            uri = "/assets/" + pair.getSecond();
        }
        URL url = getClass().getResource(uri);
        if (url == null) {
            url = getClass().getResource("/assets/defaults" + uri.substring(7));
        }
        if (url == null) {
            // If the file is not found not is a .css file, check if we need to generate it via a .scss file
            if (uri.endsWith(".css")) {
                String scssUri = uri.substring(0, uri.length() - 4) + ".scss";
                url = getClass().getResource(scssUri);
                if (url != null) {
                    handleSASS(ctx, uri, scssUri, url);
                    return true;
                }
            }
            // If the file is non existent, check if we can generate it by using a velocity template
            url = getClass().getResource(uri + ".vm");
            if (url != null) {
                handleVM(ctx, uri, url);
                return true;
            }
        }
        if (url == null) {
            ctx.respondWith().error(HttpResponseStatus.NOT_FOUND);
        } else if ("file".equals(url.getProtocol())) {
            ctx.respondWith().file(new File(url.toURI()));
        } else {
            ctx.respondWith().resource(url.openConnection());
        }
        return true;
    }

    private static final Log SASS_LOG = Log.get("sass");

    /*
     * Subclass of generator which takes care of proper logging
     */
    private static class SIRIUSGenerator extends Generator {
        @Override
        public void debug(String message) {
            SASS_LOG.FINE(message);
        }

        @Override
        public void warn(String message) {
            SASS_LOG.WARN(message);
        }
    }

    @Part
    private Content content;

    /*
     * Uses Velocity (via the content generator) to generate the desired file
     */
    private void handleVM(WebContext ctx, String uri, URL url) throws IOException {
        URLConnection connection = url.openConnection();
        String cacheKey = uri.substring(1).replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
        File file = new File(getCacheDirFile(), cacheKey);

        if (!file.exists() || file.lastModified() < connection.getLastModified()) {
            try {
                if (Sirius.isDev()) {
                    Content.LOG.INFO("Compiling: " + uri + ".vm");
                }
                FileOutputStream out = new FileOutputStream(file, false);
                content.generator().useTemplate(uri + ".vm").generateTo(out);
                out.close();
            } catch (Throwable t) {
                file.delete();
                ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(Content.LOG, t));
            }
        }

        ctx.respondWith().named(uri.substring(uri.lastIndexOf("/") + 1)).file(file);
    }

    /*
     * Uses server-sass to compile a SASS file (.scss) into a .css file
     */
    private void handleSASS(WebContext ctx, String cssUri, String scssUri, URL url) throws IOException {
        URLConnection connection = url.openConnection();
        String cacheKey = cssUri.substring(1).replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
        File file = new File(getCacheDirFile(), cacheKey);

        if (!file.exists() || file.lastModified() < connection.getLastModified()) {
            if (Sirius.isDev()) {
                SASS_LOG.INFO("Compiling: " + scssUri);
            }
            try {
                SIRIUSGenerator gen = new SIRIUSGenerator();
                gen.importStylesheet(scssUri);
                gen.compile();
                FileWriter writer = new FileWriter(file, false);
                // Let the content compressor take care of minifying the CSS
                Output out = new Output(writer, false);
                gen.generate(out);
                writer.close();
            } catch (Throwable t) {
                file.delete();
                ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(SASS_LOG, t));
            }
        }

        ctx.respondWith().named(cssUri.substring(cssUri.lastIndexOf("/") + 1)).file(file);
    }

    /*
     * Resolves the directory used to cache the generated files
     */
    private File getCacheDirFile() {
        if (cacheDirFile == null) {
            cacheDirFile = new File(cacheDir);
            if (!cacheDirFile.exists()) {
                WebServer.LOG.INFO("Cache directory for generated content does not exist! Creating: %s", cacheDir);
                cacheDirFile.mkdirs();
            }
        }
        return cacheDirFile;
    }
}
