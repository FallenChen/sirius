/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.templates;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.rythmengine.Rythm;
import org.rythmengine.RythmEngine;
import org.rythmengine.conf.RythmConfigurationKey;
import org.rythmengine.extension.II18nMessageResolver;
import org.rythmengine.extension.ISourceCodeEnhancer;
import org.rythmengine.extension.ITemplateResourceLoader;
import org.rythmengine.internal.compiler.TemplateClass;
import org.rythmengine.resource.ITemplateResource;
import org.rythmengine.resource.TemplateResourceManager;
import org.rythmengine.template.ITemplate;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.BasicCollector;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;
import sirius.web.controller.UserContext;
import sirius.web.http.WebContext;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Initializes and configures Rythm (http://www.rythmengine.org).
 * <p>
 * Configures Rythm, so that {@literal @}i18n uses {@link NLS#get(String)}. Also the template lookup is changed
 * to scan resources/view/... or resources/help/...
 * </p>
 * <p>
 * Each template will have two auto-import: {@link NLS} and {@link sirius.kernel.commons.Strings}. Additionally,
 * the following variables are declared:
 * <ul>
 * <li><b>ctx</b>: the current {@link CallContext}</li>
 * <li><b>user</b>: the current {@link UserContext}</li>
 * <li><b>prefix</b>: the http url prefix</li>
 * <li><b>product</b>: the name of the product</li>
 * <li><b>version</b>: the version of the product</li>
 * <li><b>isDev</b>: <tt>true</tt> if the system is started in development mode, <tt>false</tt> otherwise</li>
 * <li><b>call</b>:the current {@link WebContext}</li>
 * </ul>
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Register
public class RythmConfig implements Lifecycle {

    public static final Log LOG = Log.get("rythm");

    /*
     * Adapter to make @i18n commands used NLS.get
     */
    public static class I18nResourceResolver implements II18nMessageResolver {

        @Override
        public String getMessage(ITemplate template, String key, Object... args) {
            return NLS.apply(key, args);
        }
    }

    @Parts(RythmExtension.class)
    private Collection<RythmExtension> extensions;

    @Override
    public void started() {
        Map<String, Object> config = Maps.newTreeMap();
        config.put("rythm.engine.mode", Sirius.isDev() ? "dev" : "prod");
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), CallContext.getCurrent().getNodeName() + "_rythm");
        tmpDir.mkdirs();
        if (Sirius.isDev()) {
            for (File file : tmpDir.listFiles()) {
                if (file.getName().endsWith(".java") || file.getName().endsWith(".rythm")) {
                    file.delete();
                }
            }
        }
        config.put("rythm.home.tmp.dir", tmpDir.getAbsolutePath());
        config.put("rythm.i18n.message.resolver.impl", I18nResourceResolver.class.getName());
        config.put(RythmConfigurationKey.RESOURCE_LOADER_IMPL.getKey(), new ITemplateResourceLoader() {

            @Override
            public ITemplateResource load(String path) {
                return new RythmTemplate(path, null);
            }

            @Override
            public TemplateClass tryLoadTemplate(String tmplName,
                                                 RythmEngine engine,
                                                 TemplateClass callerTemplateClass) {
                if (!tmplName.startsWith("view") && !tmplName.startsWith("help")) {
                    return null;
                }
                String ext = Files.getFileExtension(tmplName);
                String file = tmplName.substring(0, tmplName.length() - ext.length() - 1);
                ITemplateResource tr = load(file.replace(".", "/") + "." + ext);
                if (!tr.isValid()) {
                    return null;
                }
                TemplateClass tc = engine.classes().getByTemplate(tr.getKey());
                if (tc != null && engine.getRegisteredTemplateClass(tc.getFullName()) == null) {
                    tc = null;
                }
                if (tc == null) {
                    tc = new TemplateClass(tr, engine);
                    try {
                        tc.asTemplate(engine);
                    } catch (Exception e) {
                        throw Exceptions.handle(e);
                    }
                }
                return tc;
            }

            @Override
            public String getFullName(TemplateClass tc, RythmEngine engine) {
                return String.valueOf(tc.getKey()).replace("/", ".");
            }

            @Override
            public void scan(String root, TemplateResourceManager manager) {
            }

        });
        config.put(RythmConfigurationKey.CODEGEN_SOURCE_CODE_ENHANCER.getKey(), new ISourceCodeEnhancer() {
            @Override
            public List<String> imports() {
                List<String> result = Lists.newArrayList();
                result.add("sirius.kernel.commons.Strings");
                result.add("sirius.kernel.nls.NLS");

                return result;
            }

            @Override
            public String sourceCode() {
                return "";
            }

            @Override
            public Map<String, ?> getRenderArgDescriptions() {
                final Map<String, Object> map = Maps.newTreeMap();
                map.put("ctx", CallContext.class);
                map.put("user", UserContext.class);
                map.put("prefix", String.class);
                map.put("product", String.class);
                map.put("version", String.class);
                map.put("isDev", Boolean.class);
                map.put("call", WebContext.class);
                for (RythmExtension ext : extensions) {
                    ext.collectExtensionNames(new BasicCollector<Tuple<String, Class<?>>>() {
                        @Override
                        public void add(Tuple<String, Class<?>> entity) {
                            map.put(entity.getFirst(), entity.getSecond());
                        }
                    });
                }
                return map;
            }

            @Override
            public void setRenderArgs(final ITemplate template) {
                CallContext ctx = CallContext.getCurrent();
                ctx.addToMDC("template", template.__getTemplateClass(true).getFullName());
                WebContext wc = ctx.get(WebContext.class);

                template.__setRenderArg("ctx", ctx);
                template.__setRenderArg("user", ctx.get(UserContext.class));
                template.__setRenderArg("prefix", wc.getContextPrefix());
                template.__setRenderArg("product", Sirius.getProductName());
                template.__setRenderArg("version", Sirius.getProductVersion());
                template.__setRenderArg("isDev", Sirius.isDev());
                template.__setRenderArg("call", wc);
                for (RythmExtension ext : extensions) {
                    ext.collectExtensionValues(new BasicCollector<Tuple<String, Object>>() {
                        @Override
                        public void add(Tuple<String, Object> entity) {
                            template.__setRenderArg(entity.getFirst(), entity.getSecond());
                        }
                    });
                }
            }
        });
        Rythm.init(config);

    }

    @Override
    public void stopped() {
    }

    @Override
    public void awaitTermination() {
        // Not necessary
    }

    @Override
    public String getName() {
        return "templates (Rythm)";
    }
}
