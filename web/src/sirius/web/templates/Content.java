/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.templates;

import com.google.common.base.Charsets;
import com.typesafe.config.ConfigValue;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

@Register(classes = {Content.class, Lifecycle.class})
public class Content implements Lifecycle {

    public static final String ENCODING = "encoding";
    public static Log LOG = Log.get("content-generator");

    @Part
    private Collection<ContentHandler> handlers;

    @Part
    private Collection<ContentContextExtender> extenders;

    @sirius.kernel.di.std.Context
    private GlobalContext ctx;

    public class Generator {

        private String templateName;
        private String velocityString;
        private String handlerType;
        private Context context = Context.create();
        private String encoding;

        public Generator applyContext(Context ctx) {
            context.putAll(ctx);
            return this;
        }

        public Generator put(String key, Object value) {
            context.put(key, value);
            return this;
        }

        public Generator encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public Generator useTemplate(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Generator direct(String velocityString) {
            this.velocityString = velocityString;
            return this;
        }

        public Generator handler(String handlerType) {
            this.handlerType = handlerType;
            return this;
        }

        public void generateTo(OutputStream out) {
            if (Strings.isFilled(templateName)) {
                CallContext.getCurrent().addToMDC("content-generator-template", templateName);
            }
            try {
                try {
                    if (Strings.isFilled(handlerType)) {
                        ContentHandler handler = ctx.findPart(handlerType, ContentHandler.class);
                        if (!handler.generate(this, out)) {

                        }
                    }
                    for (ContentHandler handler : handlers) {
                        if (handler.generate(this, out)) {
                            return;
                        }
                    }
                } catch (HandledException e) {
                    throw e;
                } catch (Throwable e) {
                    throw Exceptions.handle()
                                    .error(e)
                                    .to(LOG)
                                    .withSystemErrorMessage("Error applying template '%s': %s (%s)",
                                                            Strings.isEmpty(templateName) ? velocityString : templateName)
                                    .handle();
                }
            } finally {
                CallContext.getCurrent().removeFromMDC("content-generator-template");
            }
        }

        public String generate() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            generateTo(out);
            return new String(out.toByteArray(), Charsets.UTF_8);
        }

        public String getTemplateName() {
            return templateName;
        }

        public String getVelocityString() {
            return velocityString;
        }

        public Context getContext() {
            return context;
        }

        public boolean isTemplateEndsWith(String extension) {
            return Strings.isFilled(templateName) && templateName.endsWith(extension);
        }

        public String getEncoding() {
            if (Strings.isFilled(encoding)) {
                return encoding;
            }
            if (context.containsKey(ENCODING)) {
                return (String) context.get(ENCODING);
            }
            return Charsets.UTF_8.name();
        }

        public String getHandlerType() {
            return handlerType;
        }

        public InputStream getTemplate() {
            return VelocityResourceLoader.INSTANCE.getResourceStream(templateName);
        }
    }

    public Generator generator() {
        Generator result = new Generator();
        for (ContentContextExtender extender : extenders) {
            extender.extend(result.getContext());
        }

        return result;
    }

    @Override
    public void started() {
        try {
            Velocity.setProperty("sirius.resource.loader.class", VelocityResourceLoader.class.getName());
            Velocity.setProperty(RuntimeConstants.RESOURCE_MANAGER_CACHE_CLASS, VelocityResourceCache.class.getName());
            Velocity.setProperty(Velocity.RESOURCE_LOADER, "sirius");
            StringBuilder libraryPath = new StringBuilder();
            for (Map.Entry<String, ConfigValue> e : Sirius.getConfig()
                                                          .getConfig("content.velocity-libraries")
                                                          .entrySet()) {
                libraryPath.append(e.getValue().render());
                libraryPath.append(",");
            }
            Velocity.setProperty(Velocity.VM_LIBRARY, libraryPath.toString());
            Velocity.setProperty(Velocity.SET_NULL_ALLOWED, Boolean.TRUE);
            Velocity.init();
        } catch (Throwable e) {
            Exceptions.handle(LOG, e);
        }
    }

    @Override
    public void stopped() {

    }

    @Override
    public void awaitTermination() {

    }

    @Override
    public String getName() {
        return "content-generator";
    }
}
