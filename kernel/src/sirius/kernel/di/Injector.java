package sirius.kernel.di;

import sirius.kernel.Classpath;
import sirius.kernel.commons.Callback;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides access to the present OCM implementation.
 */
public class Injector {

    /**
     * Logger used by the injection framework. This is public so that annotation processors in sub packages can
     * log through this logger.
     */
    public static final Log LOG = Log.get("di");

    private static PartRegistry ctx = new PartRegistry();

    /**
     * Initializes the framework...
     */
    public static void init(Callback<MutableGlobalContext> callback, Classpath classpath) {
        ctx = new PartRegistry();

        final List<Class<?>> classes = new ArrayList<Class<?>>();
        final List<ClassLoadAction> actions = new ArrayList<ClassLoadAction>();
        LOG.INFO("Initializing the MicroKernel....");

        LOG.INFO("Stage 1: Scanning .class files...");
        classpath.find(Pattern.compile(".*?.class"), new Callback<Matcher>() {
            @Override
            public void invoke(Matcher matcher) throws Exception {
                String relativePath = matcher.group();
                String className = relativePath.substring(0, relativePath.length() - 6).replace("/", ".");
                try {
                    LOG.FINE("Found class: " + className);
                    Class<?> clazz = Class.forName(className);
                    if (ClassLoadAction.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        try {
                            actions.add((ClassLoadAction) clazz.newInstance());
                        } catch (Throwable e) {
                            Exceptions.handle()
                                      .error(e)
                                      .to(LOG)
                                      .withSystemErrorMessage("Failed to instantiate ClassLoadAction: %s - %s (%s)",
                                                              className)
                                      .handle();
                        }
                    }
                    classes.add(clazz);
                } catch (NoClassDefFoundError e) {
                    Exceptions.handle()
                              .error(e)
                              .to(LOG)
                              .withSystemErrorMessage("Failed to load dependent class: %s", className)
                              .handle();
                } catch (Throwable e) {
                    Exceptions.handle()
                              .error(e)
                              .to(LOG)
                              .withSystemErrorMessage("Failed to load class %s: %s (%s)", className)
                              .handle();
                }
            }
        });

        LOG.INFO("Stage 2: Applying %d class load actions on %d classes...", actions.size(), classes.size());
        for (Class<?> clazz : classes) {
            for (ClassLoadAction action : actions) {
                if (action.getTrigger() == null || clazz.isAnnotationPresent(action.getTrigger())) {
                    LOG.FINE("Autoinstalling class: %s based on %s", clazz.getName(), action.getClass().getName());
                    try {
                        action.handle(ctx, clazz);
                    } catch (Throwable e) {
                        Exceptions.handle()
                                  .error(e)
                                  .to(LOG)
                                  .withSystemErrorMessage("Failed to autoload: %s with ClassLoadAction: %s: %s (%s)",
                                                          clazz.getName(),
                                                          action.getClass().getSimpleName())
                                  .handle();
                    }
                }
            }
        }

        LOG.INFO("Stage 3: Enhancing context");
        if (callback != null) {
            try {
                callback.invoke(ctx);
            } catch (Exception e) {
                LOG.SEVERE(e);
            }
        }

        LOG.INFO("Stage 4: Initializing parts...");
        ctx.processAnnotations();

        LOG.INFO("Stage 5: Initializing static parts-references...");
        for (Class<?> clazz : classes) {
            ctx.wireClass(clazz);
        }
    }

    /**
     * Provides access to the service model.
     */
    public static GlobalContext context() {
        return ctx;
    }

}
