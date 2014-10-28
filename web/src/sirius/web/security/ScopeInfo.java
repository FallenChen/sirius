/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import java.util.function.Function;

/**
 * Created by aha on 20.06.14.
 */
public class ScopeInfo {

    public static final ScopeInfo DEFAULT_SCOPE = new ScopeInfo("default", "default", "default", null);

    private String scopeId;
    private String scopeType;
    private String scopeName;
    private Function<ScopeInfo, Object> scopeSupplier;

    public ScopeInfo(String scopeId, String scopeType, String scopeName, Function<ScopeInfo, Object> scopeSupplier) {
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.scopeName = scopeName;
        this.scopeSupplier = scopeSupplier;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeName() {
        return scopeName;
    }

    @SuppressWarnings("unchecked")
    public <T> T getScopeObject(Class<T> clazz) {
        if (scopeSupplier == null) {
            return null;
        }
        Object scope = scopeSupplier.apply(this);
        if (scope != null && clazz.isAssignableFrom(scope.getClass())) {
            return (T) scope;
        }
        return null;
    }

}
