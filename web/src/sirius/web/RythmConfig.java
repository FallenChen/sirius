package sirius.web;

import org.rythmengine.Rythm;
import org.rythmengine.extension.II18nMessageResolver;
import org.rythmengine.template.ITemplate;
import sirius.kernel.Sirius;
import sirius.kernel.nls.NLS;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.async.CallContext;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 23:10
 * To change this template use File | Settings | File Templates.
 */
@Register
public class RythmConfig implements Lifecycle {

    public static class I18nResourceResolver implements II18nMessageResolver {

        @Override
        public String getMessage(ITemplate template, String key, Object... args) {
            return NLS.apply(key, args);
        }
    }

    @Override
    public void started() {
        Map<String, String> config = new TreeMap<String, String>();
        config.put("rythm.engine.mode", Sirius.isDev() ? "dev" : "prod");
        config.put("rythm.home.template.dir",
                   new File(Thread.currentThread()
                                  .getContextClassLoader()
                                  .getResource("component.conf")
                                  .getFile()).getParent());
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), CallContext.getCurrent().getNodeName() + "_rythm");
        tmpDir.mkdirs();
        config.put("rythm.home.tmp.dir", tmpDir.getAbsolutePath());
        config.put("rythm.i18n.message.resolver.impl", I18nResourceResolver.class.getName());
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
