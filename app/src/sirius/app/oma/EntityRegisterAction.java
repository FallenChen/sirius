/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.oma;

import sirius.app.oma.annotations.Table;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.ClassLoadAction;
import sirius.kernel.di.MutableGlobalContext;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.01.14
 * Time: 13:20
 * To change this template use File | Settings | File Templates.
 */
public class EntityRegisterAction implements ClassLoadAction {
    @Nullable
    @Override
    public Class<? extends Annotation> getTrigger() {
        return Table.class;
    }

    @Override
    public void handle(@Nonnull MutableGlobalContext ctx, @Nonnull Class<?> clazz) throws Exception {
        Table t = clazz.getAnnotation(Table.class);
        if (Strings.isFilled(t.framework()) && !Sirius.isFrameworkEnabled(t.framework())) {
            return;
        }
        try {
            ctx.registerPart(clazz.newInstance(), Entity.class);
        } catch (Throwable e) {
            Exceptions.handle()
                      .error(e)
                      .withSystemErrorMessage("Cannot register %s as entity class: %s (%s)", clazz)
                      .to(OMA.LOG);
        }
    }
}
