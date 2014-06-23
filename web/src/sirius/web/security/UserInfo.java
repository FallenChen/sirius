/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import sirius.kernel.commons.Strings;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by aha on 20.06.14.
 */
public class UserInfo {

    public static final String PERMISSION_LOGGED_IN = "flag-logged-in";

    public static final UserInfo NOBODY = new UserInfo("ANONYMOUS", "(no user)", "", null, null);
    public static final UserInfo GOD_LIKE = new UserInfo("ADMIN", "(admin)", "", Collections.singleton("*"), null);

    private String userId;
    private String username;
    private String eMail;
    private Set<String> permissions = null;
    private boolean allPermissions = false;
    private Function<UserInfo, Object> userSupplier;

    public UserInfo(String userId,
                    String username,
                    String eMail,
                    Set<String> permissions,
                    Function<UserInfo, Object> userSupplier) {
        this.userId = userId;
        this.username = username;
        this.eMail = eMail;
        this.permissions = permissions;
        this.allPermissions = permissions != null && permissions.contains("*");
        this.userSupplier = userSupplier;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return username;
    }

    public String getEmail() {
        return eMail;
    }

    public boolean hasPermission(String permission) {
        if (Strings.isEmpty(permission)) {
            return true;
        }
        if (permission.startsWith("!")) {
            return permissions == null || !permissions.contains(permission.substring(1));
        } else {
            return allPermissions || (permissions != null && permissions.contains(permission));
        }
    }

    public boolean isLoggedIn() {
        return hasPermission(PERMISSION_LOGGED_IN);
    }

    public <T> T getUserObject(Class<T> clazz) {
        if (userSupplier == null) {
            return null;
        }
        Object user = userSupplier.apply(this);
        if (user != null && clazz.isAssignableFrom(user.getClass())) {
            return (T) user;
        }
        return null;
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }
}
