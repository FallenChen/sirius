package sirius.kernel.di.std;

import sirius.kernel.di.AnnotationProcessor;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.di.annotations.ExtensionList;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.extensions.Extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Register
public class ExtensionListAnnotationProcessor implements AnnotationProcessor {
    @Override
    public Class<? extends Annotation> getTrigger() {
        return ExtensionList.class;
    }

    @Override
    public void handle(MutableGlobalContext ctx, Object object, Field field) throws Exception {
        ExtensionList val = field.getAnnotation(ExtensionList.class);
        field.set(object, Extensions.getExtensions(val.value()));
    }
}
