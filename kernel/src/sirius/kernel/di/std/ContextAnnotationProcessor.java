package sirius.kernel.di.std;

import sirius.kernel.di.AnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.annotations.Context;
import sirius.kernel.di.annotations.Register;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Register
public class ContextAnnotationProcessor implements AnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Context.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        field.set(object, ctx);
    }
}
