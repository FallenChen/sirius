package sirius.web.templates;

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
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;
import sirius.web.controller.UserContext;
import sirius.web.http.WebContext;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 23:10
 * To change this template use File | Settings | File Templates.
 */
@Register
public class RythmConfig implements Lifecycle {

    public static final Log LOG = Log.get("rythm");

    public static class I18nResourceResolver implements II18nMessageResolver {

        @Override
        public String getMessage(ITemplate template, String key, Object... args) {
            return NLS.apply(key, args);
        }
    }

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
                if (!tmplName.startsWith("view")) {
                    return null;
                }
                String ext = Files.getFileExtension(tmplName);
                String file = tmplName.substring(0, tmplName.length() - ext.length() - 1);
                ITemplateResource tr = load(file.replace(".", "/") + "." + ext);
                if (!tr.isValid()) {
                    return null;
                }
                TemplateClass tc = engine.classes().getByTemplate(tr.getKey());
                if (null == tc) {
                    tc = new TemplateClass(tr, engine);
                    try {
                        tc.asTemplate(engine);
                        //engine.registerTemplateClass(tc);
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
                return Collections.emptyList();
            }

            @Override
            public String sourceCode() {
                return "";
            }

            @Override
            public Map<String, ?> getRenderArgDescriptions() {
                Map<String, Object> map = Maps.newTreeMap();
                map.put("ctx", CallContext.class);
                map.put("user", UserContext.class);
                map.put("prefix", String.class);
                map.put("product", String.class);
                map.put("version", String.class);
                map.put("isDev", Boolean.class);
                map.put("call", WebContext.class);
                return map;
            }

            @Override
            public void setRenderArgs(ITemplate template) {
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
            }
        });
        Rythm.init(config);

    }

    @Override
    public void stopped() {
    }

    @Override
    public String getName() {
        return "templates (Rythm)";
    }
}
