/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.web.http.WebContext;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by aha on 20.06.14.
 */
public class SSOUserManager extends GenericUserManager {

    @Register(name = "sso")
    public static class PublicUserManagerFactory implements UserManagerFactory {

        @Nonnull
        @Override
        public UserManager createManager(@Nonnull ScopeInfo scope, @Nonnull Extension config) {
            return new SSOUserManager(scope, config);
        }

    }

    private final boolean parseRoles;

    protected SSOUserManager(ScopeInfo scope, Extension config) {
        super(scope, config);
        if (sessionStorage == SESSION_STORAGE_TYPE_CLIENT) {
            UserContext.LOG.WARN(
                    "SSOUserManager (sso) for scope %s does not support 'client' as session type! Switching to 'server'.",
                    scope.getScopeType());
            sessionStorage = SESSION_STORAGE_TYPE_SERVER;
        }
        parseRoles = config.get("parse-roles").asBoolean(true);
    }

    @Override
    protected String computeSSOHashInput(WebContext ctx, String user, Tuple<String, String> challengeResponse) {
        if (ctx.get("roles").isFilled()) {
            return super.computeSSOHashInput(ctx, user, challengeResponse) + ctx.get("roles").asString();
        }
        return super.computeSSOHashInput(ctx, user, challengeResponse);
    }

    @Override
    protected UserInfo findUserByName(WebContext ctx, String user) {
        Set<String> roles = parseRoles ? ctx.get("roles")
                                            .asOptionalString()
                                            .map(this::parseRolesString)
                                            .orElse(Collections.emptySet()) : Collections.emptySet();
        return new UserInfo(user, user, null, transformRoles(roles), null);
    }

    private Set<String> parseRolesString(String rolesString) {
        return Arrays.asList(rolesString.split(","))
                     .stream()
                     .map(String::trim)
                     .filter(Strings::isEmpty)
                     .collect(Collectors.toSet());
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
