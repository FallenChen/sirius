/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.scripting;

import com.google.common.base.Charsets;
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

import javax.script.*;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Created by aha on 06.06.14.
 */
@Register(framework = "server-scripting")
public class ServerScriptEngine implements Initializable {

    @ConfigValue("content.script-engine")
    private String scriptEngine;
    private ScriptEngine engine;
    private SimpleScriptContext context;

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
        context = new SimpleScriptContext();

        context.setAttribute("logger", LOG, ScriptContext.ENGINE_SCOPE);
        context.setAttribute("ctx", Injector.context(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("scriptEngine", this, ScriptContext.ENGINE_SCOPE);

        Sirius.getClasspath().find(Pattern.compile("scripts/(.*?\\.js)")).map(m -> m.group(0)).forEach(m -> {
            LOG.INFO("Loading: %s", m);
            evalLibraryScript(m);
            timer.addWatchedResource(VelocityResourceLoader.INSTANCE.resolve(m), () -> evalLibraryScript(m));
        });
    }

    private void evalLibraryScript(String relativePath) {
        try {
            engine.put(ScriptEngine.FILENAME, relativePath);
            engine.eval(new InputStreamReader(VelocityResourceLoader.INSTANCE.getResourceStream(relativePath),
                                              Charsets.UTF_8), context);
        } catch (ScriptException e) {
            Exceptions.handle(e);
        }
    }
}
