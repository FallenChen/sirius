/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.nls.NLS;
import sirius.web.controller.Message;
import sirius.web.http.WebContext;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by aha on 20.06.14.
 */
public class LDAPUserManager extends GenericUserManager {

    @Register(name = "ldap")
    public static class PublicUserManagerFactory implements UserManagerFactory {

        @Nonnull
        @Override
        public UserManager createManager(@Nonnull ScopeInfo scope, @Nonnull Extension config) {
            return new LDAPUserManager(scope, config);
        }

    }

    public LDAPUserManager(ScopeInfo scope, Extension config) {
        super(scope, config);
    }

    @Override
    protected UserInfo findUserByName(WebContext ctx, String user) {
        return null;
    }

    @Override
    protected UserInfo findUserByCredentials(WebContext ctx, String user, String password) {
        return null;
    }

    @Override
    protected Object getUserObject(UserInfo u) {
        return null;
    }

}
