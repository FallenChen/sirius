/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.scripting;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import sirius.kernel.Sirius;
import sirius.kernel.di.Initializable;
import sirius.kernel.di.Injector;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.kernel.timer.TimerService;
import sirius.web.templates.VelocityResourceLoader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by aha on 06.06.14.
 */
@Register(framework = "web.server-scripting")
public class ServerScriptEngine implements Initializable {

    @ConfigValue("content.script-engine")
    private String scriptEngine;
    private ScriptEngine engine;
    private Map<String, LocalEnvironment> activeScripts = Maps.newConcurrentMap();

    public static final Log LOG = Log.get("script");

    @Part
    private TimerService timer;

    public void register(String uniqueName, Object object, String iFace) {
        try {
            Class<?> lookupClass = Class.forName(iFace);
            Invocable inv = (Invocable) engine;
            Object part = inv.getInterface(object, lookupClass);
            if (part == null) {
                throw Exceptions.handle()
                        .withSystemErrorMessage("Cannot convert JavaScript-Object '%s' into '%s'!",
                                object,
                                lookupClass)
                        .to(LOG)
                        .handle();
            }
            Injector.context().registerDynamicPart(uniqueName, part, lookupClass);
        } catch (ClassNotFoundException e) {
            throw Exceptions.handle()
                    .withSystemErrorMessage("Cannot find class: %s as dynamic part: %s (%s)", iFace)
                    .to(LOG)
                    .error(e)
                    .handle();
        }
    }

    @Override
    public void initialize() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName(scriptEngine);

        Sirius.getClasspath().find(Pattern.compile("(scripts|customizations/[^/]+/scripts)/(.*?\\.js)")).map(m -> m.group(0)).forEach(m -> {
            LOG.INFO("Loading: %s", m);
            evalLibraryScript(m);
            timer.addWatchedResource(VelocityResourceLoader.INSTANCE.resolve(m), () -> evalLibraryScript(m));
        });
    }

    private void evalLibraryScript(String relativePath) {
        LocalEnvironment env = new LocalEnvironment(relativePath);
        try {
            activeScripts.put(relativePath, env);
            engine.put(ScriptEngine.FILENAME, relativePath);
            engine.eval(new InputStreamReader(VelocityResourceLoader.INSTANCE.getResourceStream(relativePath),
                    Charsets.UTF_8), env.getContext());
        } catch (ScriptException e) {
            env.logException(Exceptions.handle(LOG, e));
        }
    }
}
