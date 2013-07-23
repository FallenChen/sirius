package sirius.kernel.di.std;

import sirius.kernel.commons.Strings;
import sirius.kernel.di.ClassLoadAction;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.annotations.Register;

import java.lang.annotation.Annotation;

/**
 * Handles the {@link sirius.kernel.di.annotations.Register} annotation.
 */
public class AutoRegisterAction implements ClassLoadAction {

    @Override
    public Class<? extends Annotation> getTrigger() {
        return Register.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Class<?> clazz) throws Exception {
        Object part = clazz.newInstance();
        Register r = clazz.getAnnotation(Register.class);
        Class<?>[] classes = r.classes();
        if (classes.length == 0) {
            classes = clazz.getInterfaces();
        }
        if (Strings.isFilled(r.name())) {
            ctx.registerPart(r.name(), part, classes);
        } else {
            ctx.registerPart(part, classes);
        }
    }

}
