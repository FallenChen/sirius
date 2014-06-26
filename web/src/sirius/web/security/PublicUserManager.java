/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.web.http.WebContext;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * Created by aha on 20.06.14.
 */
public class PublicUserManager extends GenericUserManager {

    private final UserInfo user;

    @Register(name = "public")
    public static class Factory implements UserManagerFactory {

        @Nonnull
        @Override
        public UserManager createManager(@Nonnull ScopeInfo scope, @Nonnull Extension config) {
            return new PublicUserManager(scope, config);
        }

    }

    protected PublicUserManager(ScopeInfo scope, Extension config) {
        super(scope, config);
        this.user = new UserInfo(null, null, "(public)", "(public)", "", transformRoles(Collections.emptySet()), null);
    }

    @Nonnull
    @Override
    public UserInfo bindToRequest(@Nonnull WebContext ctx) {
        return user;
    }

    @Override
    protected UserInfo findUserByName(WebContext ctx, String user) {
        return null;
    }

    @Override
    protected UserInfo findUserByCredentials(WebContext ctx, String user, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getUserObject(UserInfo u) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachToSession(@Nonnull UserInfo user, @Nonnull WebContext ctx) {

    }

    @Override
    public void detachFromSession(@Nonnull UserInfo user, @Nonnull WebContext ctx) {

    }
}
