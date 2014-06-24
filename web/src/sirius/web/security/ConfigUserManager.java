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
import com.google.common.hash.Hashing;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.web.http.WebContext;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by aha on 20.06.14.
 */
public class ConfigUserManager extends GenericUserManager {


    @Register(name = "config")
    public static class PublicUserManagerFactory implements UserManagerFactory {

        @Nonnull
        @Override
        public UserManager createManager(@Nonnull ScopeInfo scope, @Nonnull Extension config) {
            return new ConfigUserManager(scope, config);
        }

    }

    protected ConfigUserManager(ScopeInfo scope, Extension config) {
        super(scope, config);
    }

    /*
     * Local cache for computed roles (after application of profiles)
     */
    private Map<String, Set<String>> userRoles = Maps.newTreeMap();

    @Override
    protected UserInfo findUserByName(WebContext ctx, String user) {
        Extension e = Extensions.getExtension("security.users", user);
        if (e != null) {
            return getUserInfo(user, e);
        } else {
            log("Unknown user: %s", user);
        }
        return null;
    }


    @Override
    protected UserInfo findUserByCredentials(WebContext ctx, String user, String password) {
        Extension e = Extensions.getExtension("security.users", user);
        if (e != null && e.get("passwordSalt").isFilled()) {
            if (Hashing.md5()
                       .hashBytes((e.get("salt").asString() + password).getBytes(Charsets.UTF_8))
                       .toString()
                       .equals(e.get("passwordSalt"))) {
                return getUserInfo(user, e);
            }
        } else {
            if (e == null) {
                log("Unknown user: %s", user);
            } else {
                log("Invalid password for user: %s", user);
            }
        }
        return null;
    }


    @Override
    protected Object getUserObject(UserInfo u) {
        return Extensions.getExtension("security.users", u.getUserId());
    }

    private UserInfo getUserInfo(String userId, Extension e) {
        return new UserInfo(userId,
                            e.get("name").asString(),
                            e.get("email").asString(),
                            computeRoles(null, userId),
                            u -> e);
    }

    @Override
    protected Set<String> computeRoles(WebContext ctx, String userId) {
        Set<String> roles = userRoles.get(userId);
        if (roles == null) {
            Extension e = Extensions.getExtension("security.users", userId);
            if (e != null) {
                roles = transformRoles(e.get("permissions").get(List.class, Collections.emptyList()));
            } else {
                log("Unknown user: %s - Rejecting all roles!", userId);
                roles = Collections.emptySet();
            }
            userRoles.put(userId, roles);
        }
        return roles;
    }

    @Override
    protected void storeRolesForUser(UserInfo user, WebContext ctx) {
    }

    @Override
    protected void clearRolesForUser(UserInfo user, WebContext ctx) {
    }
}
