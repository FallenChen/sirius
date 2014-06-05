/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di.std;

import sirius.kernel.commons.Strings;
import sirius.kernel.di.Injector;
import sirius.kernel.di.MethodAnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

/**
 * Handles the {@link sirius.kernel.di.std.Register} annotation places on methods.
 * <p>See {@link sirius.kernel.di.std.AutoRegisterAction} on how Register annotations on class level are processes</p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.kernel.di.FieldAnnotationProcessor
 * @see sirius.kernel.di.std.Register
 * @since 2013/08
 */
@Register
public class RegisterAnnotationProcessor implements MethodAnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Register.class;
    }

    /*
     * Wrapper Method -> Supplier<?>
     */
    protected static class MethodFactory implements Supplier<Object> {

        private Method method;

        public MethodFactory(Method method) {
            this.method = method;
        }

        @Override
        public Object get() {
            try {
                return method.invoke(null);
            } catch (IllegalAccessException e) {
                throw Exceptions.handle(e);
            } catch (InvocationTargetException e) {
                throw Exceptions.handle(e);
            }
        }
    }

    @Override
    public void handle(@Nonnull MutableGlobalContext ctx,
                       @Nonnull Method method) throws Exception {
        String factoryName = method.getAnnotation(Register.class).name();
        if (Strings.isEmpty(factoryName)) {
            throw Exceptions.handle()
                            .to(Injector.LOG)
                            .withSystemErrorMessage(
                                    "Cannot process @Register annotation for method %s of class %s: The annotation must provide a factory name",
                                    method.getName(),
                                    method.getDeclaringClass().getName())
                            .handle();
        }
        if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
            throw Exceptions.handle()
                            .to(Injector.LOG)
                            .withSystemErrorMessage(
                                    "Cannot process @Register annotation for method %s of class %s as factory for %s: Method must be accessible and static",
                                    method.getName(),
                                    method.getDeclaringClass().getName(),
                                    factoryName)
                            .handle();
        }
        ctx.registerFactory(factoryName, new MethodFactory(method), method.getReturnType());
    }

}
