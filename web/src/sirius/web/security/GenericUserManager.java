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
import com.typesafe.config.Config;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.extensions.Extension;
import sirius.kernel.nls.NLS;
import sirius.web.controller.Message;
import sirius.web.http.WebContext;
import sirius.web.http.session.ServerSession;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Created by aha on 20.06.14.
 */
public abstract class GenericUserManager implements UserManager {

    protected static final long SSO_GRACE_PERIOD_IN_SECONDS = 60 * 60;
    protected static final String SESSION_STORAGE_TYPE_SERVER = "server";
    protected static final String SESSION_STORAGE_TYPE_CLIENT = "client";

    protected final ScopeInfo scope;
    protected final Extension config;
    protected String sessionStorage;
    protected boolean ssoEnabled;
    protected String ssoSecret;
    protected List<String> defaultRoles;

    protected static Map<String, Set<String>> profilesCache;

    public GenericUserManager(ScopeInfo scope, Extension config) {
        this.scope = scope;
        this.config = config;
        this.sessionStorage = config.get("session-storage").asString().intern();
        this.ssoSecret = config.get("sso-secret").asString();
        this.ssoEnabled = Strings.isFilled(ssoSecret) && config.get("sso-enabled").asBoolean(false);
        this.defaultRoles = config.get("default-roles").get(List.class, Collections.emptyList());

    }

    @Nonnull
    @Override
    public UserInfo bindToRequest(@Nonnull WebContext ctx) {
        UserInfo result = findUserInSession(ctx);
        if (result != null) {
            return result;
        }

        result = loginViaUsernameAndPassword(ctx);
        if (result != null) {
            return result;
        }

        result = loginViaSSOToken(ctx);
        if (result != null) {
            return result;
        }

        return UserInfo.NOBODY;
    }

    private UserInfo loginViaSSOToken(WebContext ctx) {
        if (!ssoEnabled) {
            return null;
        }
        if (ctx.get("user").isFilled() && ctx.get("token").isFilled()) {
            String user = ctx.get("user").trim();
            String token = ctx.get("token").trim();

            UserInfo result = findUserByName(ctx, user);
            if (result != null) {
                // An SSO token is TIMESTAMP:MD5
                Tuple<String, String> challengeResponse = Strings.split(token, ":");
                // Verify age...
                if (Value.of(challengeResponse.getFirst())
                         .asLong(0) > (System.currentTimeMillis() / 1000) - SSO_GRACE_PERIOD_IN_SECONDS) {
                    // Verify timestamp...
                    if (Hashing.md5()
                               .hashBytes(computeSSOHashInput(ctx, user, challengeResponse).getBytes(Charsets.UTF_8))
                               .toString()
                               .equals(challengeResponse.getSecond())) {
                        log("SSO-Login of %s succeeded with token: %s", user, token);
                        return result;
                    } else {
                        log("SSO-Login of %s failed due to invalid hash in token: %s", user, token);
                    }
                } else {
                    log("SSO-Login of %s failed due to outdated timestamp in token: %s", user, token);
                }
            }
            UserContext.message(Message.error(NLS.get("GenericUserManager.invalidSSO")));
        }
        return null;
    }

    protected String computeSSOHashInput(WebContext ctx, String user, Tuple<String, String> challengeResponse) {
        return ssoSecret + challengeResponse.getFirst() + user;
    }

    protected Set<String> transformRoles(Collection<String> roles) {
        Set<String> result = Sets.newTreeSet();
        for (String role : defaultRoles) {
            expand(role, result);
        }
        for (String role : roles) {
            expand(role, result);
        }
        return result;
    }

    private void expand(String role, Set<String> result) {
        if (!result.contains(role)) {
            result.add(role);
            for (String subRole : getProfile(role)) {
                expand(subRole, result);
            }
        }
    }

    protected void log(String pattern, Object... params) {
        UserContext.LOG.FINE("UserManager '%s' for scope '%s' (%s) - %s",
                             getClass().getSimpleName(),
                             scope.getScopeId(),
                             scope.getScopeType(),
                             Strings.apply(pattern, params));
    }

    private Set<String> getProfile(String role) {
        if (profilesCache == null) {
            Map<String, Set<String>> profiles = Maps.newHashMap();
            Config profilesConfig = Sirius.getConfig().atPath("security.profiles");
            profilesConfig.entrySet()
                          .stream()
                          .map(e -> e.getKey())
                          .forEach(key -> profiles.put(key, Sets.newTreeSet(profilesConfig.getStringList(key))));
            profilesCache = profiles;
        }
        return profilesCache.getOrDefault(role, Collections.emptySet());
    }

    protected abstract UserInfo findUserByName(WebContext ctx, String user);

    private UserInfo loginViaUsernameAndPassword(WebContext ctx) {
        if (ctx.get("user").isFilled() && ctx.get("password").isFilled()) {
            String user = ctx.get("user").trim();
            String password = ctx.get("password").trim();

            UserInfo result = findUserByCredentials(ctx, user, password);
            if (result != null) {
                log("Login of %s succeeded using password", user);
                return result;
            }
            log("Login of %s failed using password", user);
            UserContext.message(Message.error(NLS.get("GenericUserManager.invalidLogin")));
        }
        return null;
    }

    protected abstract UserInfo findUserByCredentials(WebContext ctx, String user, String password);

    private UserInfo findUserInSession(WebContext ctx) {
        if (sessionStorage == SESSION_STORAGE_TYPE_SERVER) {
            if (ctx.getServerSession(false).isPresent()) {
                Value userId = ctx.getServerSession().getValue(scope.getScopeId() + "-user-id");
                if (userId.isFilled()) {
                    return new UserInfo(userId.asString(),
                                        ctx.getServerSession().getValue(scope.getScopeId() + "-user-name").asString(),
                                        ctx.getServerSession().getValue(scope.getScopeId() + "-user-email").asString(),
                                        computeRoles(ctx, userId.asString()),
                                        u -> getUserObject(u));
                }
            }
        } else if (sessionStorage == SESSION_STORAGE_TYPE_CLIENT) {
            Value userId = ctx.getSessionValue(scope.getScopeId() + "-user-id");
            if (userId.isFilled()) {
                return new UserInfo(userId.asString(),
                                    ctx.getSessionValue(scope.getScopeId() + "-user-name").asString(),
                                    ctx.getSessionValue(scope.getScopeId() + "-user-email").asString(),
                                    computeRoles(ctx, userId.asString()),
                                    u -> getUserObject(u));

            }
        }
        return null;
    }

    protected Set<String> computeRoles(WebContext ctx, String userId) {
        if (sessionStorage == SESSION_STORAGE_TYPE_SERVER) {
            return ctx.getServerSession()
                      .getValue(scope.getScopeId() + "-user-roles")
                      .get(Set.class, Collections.emptySet());
        } else {
            return Collections.emptySet();
        }
    }

    protected abstract Object getUserObject(UserInfo u);

    @Override
    public void attachToSession(@Nonnull UserInfo user, @Nonnull WebContext ctx) {
        if (sessionStorage == SESSION_STORAGE_TYPE_SERVER) {
            ServerSession sess = ctx.getServerSession();
            sess.putValue(scope.getScopeId() + "-user-id", user.getUserId());
            sess.putValue(scope.getScopeId() + "-user-name", user.getUserName());
            sess.putValue(scope.getScopeId() + "-user-email", user.getEmail());
        } else if (sessionStorage == SESSION_STORAGE_TYPE_CLIENT) {
            ctx.setSessionValue(scope.getScopeId() + "-user-id", user.getUserId());
            ctx.setSessionValue(scope.getScopeId() + "-user-name", user.getUserName());
            ctx.setSessionValue(scope.getScopeId() + "-user-email", user.getEmail());
        }
        storeRolesForUser(user, ctx);
    }

    protected void storeRolesForUser(UserInfo user, WebContext ctx) {
        if (sessionStorage == SESSION_STORAGE_TYPE_SERVER) {
            Optional<ServerSession> sess = ctx.getServerSession(false);
            if (sess.isPresent()) {
                sess.get().putValue(scope.getScopeId() + "-user-roles", user.getPermissions());
            }
        }
    }

    @Override
    public void detachFromSession(@Nonnull UserInfo user, @Nonnull WebContext ctx) {
        if (sessionStorage == "server") {
            Optional<ServerSession> s = ctx.getServerSession(false);
            if (s.isPresent()) {
                ServerSession sess = s.get();
                sess.putValue(scope.getScopeId() + "-user-id", null);
                sess.putValue(scope.getScopeId() + "-user-name", null);
                sess.putValue(scope.getScopeId() + "-user-email", null);
                sess.putValue(scope.getScopeId() + "-user-roles", null);
            }
        } else if (sessionStorage == "client") {
            ctx.setSessionValue(scope.getScopeId() + "-user-id", null);
            ctx.setSessionValue(scope.getScopeId() + "-user-name", null);
            ctx.setSessionValue(scope.getScopeId() + "-user-email", null);
        }
        clearRolesForUser(user, ctx);
    }

    protected void clearRolesForUser(UserInfo user, WebContext ctx) {
        if (sessionStorage == "server") {
            Optional<ServerSession> sess = ctx.getServerSession(false);
            if (sess.isPresent()) {
                sess.get().putValue(scope.getScopeId() + "-user-roles", null);
            }
        }
    }
}
