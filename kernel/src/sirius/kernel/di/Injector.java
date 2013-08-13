/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import sirius.kernel.Classpath;
import sirius.kernel.commons.BasicCollector;
import sirius.kernel.commons.Callback;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central class for collecting and injecting dependencies (which are called <b>parts</b>).
 * <p>
 * Contains the micro kernel which keeps track of all parts and injects them where required. We consider this
 * a micro kernel, as it doesn't directly know any annotations. It rather delegates the work to classes which implement
 * defined interfaces. Therefore the implementation is rather short but easily extensible.
 * </p>
 * <p>
 * Parts are commonly added via subclasses for {@link ClassLoadAction}. These scan each class in the classpath
 * and instantiate and insert them into the {@link MutableGlobalContext} if required. Most of these
 * <tt>ClassLoadAction</tt> implementations trigger on annotations. A prominent example is the
 * {@link sirius.kernel.di.std.AutoRegisterAction} which loads all classes wearing the {@link sirius.kernel.di.std.Register}
 * annotation. Subclasses of <tt>ClassLoadAction</tt> are discovered automatically.
 * </p>
 * <p>
 * Accessing parts can be done in two ways. First, one can access the current {@link GlobalContext} via
 * {@link #context()}. This can be used to retrieve parts by class or by class and name. The second way
 * to access parts is to used marker annotations like {@link sirius.kernel.di.std.Part},
 * {@link sirius.kernel.di.std.Parts} or {@link sirius.kernel.di.std.Context}. Again,
 * these annotations are not processed by the micro kernel itself, but by subclasses of {@link AnnotationProcessor}.
 * To process all annotations of a given java object, {@link GlobalContext#wire(Object)} can be used. This will
 * be automatically called for each part which is auto-instantiated by a <tt>ClassLoadAction</tt>.
 * </p>
 * <p>
 * Also all annotations on static fields are processed on system startup. This is a simple trick to pass a
 * part to objects which are frequently created and destroyed.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 1.0
 */
public class Injector {

    /**
     * Logger used by the injection framework. This is public so that annotation processors in sub packages can
     * log through this logger.
     */
    public static final Log LOG = Log.get("di");

    private static PartRegistry ctx = new PartRegistry();

    /**
     * Initializes the framework. Must be only called once on system startup.
     *
     * @param callback  the given callback is invoked, once the system is initialized and permits to add external
     *                  parts to the given context.
     * @param classpath the classpath used to enumerate all classes to be scanned
     */
    public static void init(@Nullable Callback<MutableGlobalContext> callback, @Nonnull final Classpath classpath) {
        ctx = new PartRegistry();

        final List<Class<?>> classes = new ArrayList<Class<?>>();
        final List<ClassLoadAction> actions = new ArrayList<ClassLoadAction>();
        LOG.INFO("Initializing the MicroKernel....");

        LOG.INFO("Stage 1: Scanning .class files...");
        classpath.find(Pattern.compile(".*?.class"), new BasicCollector<Matcher>() {
            @Override
            public void add(Matcher matcher) {
                String relativePath = matcher.group();
                String className = relativePath.substring(0, relativePath.length() - 6).replace("/", ".");
                try {
                    LOG.FINE("Found class: " + className);
                    Class<?> clazz = Class.forName(className, true, classpath.getLoader());
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
                    LOG.FINE("Auto-installing class: %s based on %s", clazz.getName(), action.getClass().getName());
                    try {
                        action.handle(ctx, clazz);
                    } catch (Throwable e) {
                        Exceptions.handle()
                                  .error(e)
                                  .to(LOG)
                                  .withSystemErrorMessage("Failed to auto-load: %s with ClassLoadAction: %s: %s (%s)",
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
     * Provides access to the global context, containing all parts
     * <p>
     * This can also be loaded into a class field using the {@link sirius.kernel.di.std.Context} annotation
     * </p>
     *
     * @return the global context containing all parts known to the system
     */
    public static GlobalContext context() {
        return ctx;
    }

}
