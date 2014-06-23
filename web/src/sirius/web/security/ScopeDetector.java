/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import sirius.web.http.WebContext;

import javax.annotation.Nonnull;

/**
 * Created by aha on 20.06.14.
 */
public interface ScopeDetector {

    @Nonnull
    ScopeInfo detectScope(@Nonnull WebContext request);

}
