package sirius.web.templates;

import com.google.common.collect.Maps;
import org.rythmengine.Rythm;
import org.rythmengine.conf.RythmConfigurationKey;
import org.rythmengine.extension.II18nMessageResolver;
import org.rythmengine.extension.ISourceCodeEnhancer;
import org.rythmengine.template.ITemplate;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;
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
        config.put("rythm.home.template.dir",
                   new File(Thread.currentThread()
                                  .getContextClassLoader()
                                  .getResource("component.marker")
                                  .getFile()).getParent());
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), CallContext.getCurrent().getNodeName() + "_rythm");
        tmpDir.mkdirs();
        config.put("rythm.home.tmp.dir", tmpDir.getAbsolutePath());
        config.put("rythm.i18n.message.resolver.impl", I18nResourceResolver.class.getName());
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
                map.put("prefix", String.class);
                map.put("product", String.class);
                map.put("version", String.class);
                map.put("call", WebContext.class);
                return map;
            }

            @Override
            public void setRenderArgs(ITemplate template) {
                CallContext ctx = CallContext.getCurrent();
                ctx.addToMDC("template", template.__getTemplateClass(true).getFullName());
                WebContext wc = ctx.get(WebContext.class);

                template.__setRenderArg("ctx", ctx);
                template.__setRenderArg("prefix", wc.getContextPrefix());
                template.__setRenderArg("product", Sirius.getProductName());
                template.__setRenderArg("version", Sirius.getProductVersion());
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
