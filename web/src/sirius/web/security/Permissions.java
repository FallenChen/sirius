/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import sirius.kernel.Sirius;
import sirius.kernel.di.std.ConfigValue;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

/**
 * Created by aha on 24.06.14.
 */
public class Permissions {

    protected static Map<String, Set<String>> profilesCache;

    @ConfigValue("security.public-roles")
    protected static List<String> publicRoles;

    private static Set<String> getProfile(String role) {
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

    private static void expand(String role, Set<String> result) {
        if (!result.contains(role)) {
            result.add(role);
            for (String subRole : getProfile(role)) {
                expand(subRole, result);
            }
        }
    }

    public static Set<String> applyProfiles(Collection<String> roles) {
        Set<String> result = Sets.newTreeSet();
        for (String role : roles) {
            expand(role, result);
        }
        return result;

    }

    public static Set<String> applyProfilesAndPublicRoles(Collection<String> roles) {
        Set<String> allRoles = Sets.newTreeSet(roles);
        if (publicRoles != null) {
            allRoles.addAll(publicRoles);
        }
        return applyProfiles(allRoles);
    }

    public static Set<String> computePermissionsFromAnnotations(AnnotatedElement object) {
        if (object.isAnnotationPresent(Permission.class) || object.isAnnotationPresent(NotPermission.class) || object.isAnnotationPresent(
                LoginRequired.class)) {
            Set<String> result = Sets.newTreeSet();
            for (Permission p : object.getAnnotationsByType(Permission.class)) {
                result.add(p.value());
            }
            for (NotPermission p : object.getAnnotationsByType(NotPermission.class)) {
                result.add("!" + p.value());
            }
            if (object.isAnnotationPresent(LoginRequired.class)) {
                result.add(UserInfo.PERMISSION_LOGGED_IN);
            }
            return result;
        }
        return Collections.emptySet();
    }
}
