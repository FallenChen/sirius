/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.frameworks.users;

import com.google.common.hash.Hashing;
import sirius.app.oma.Entity;
import sirius.app.oma.One;
import sirius.app.oma.annotations.*;
import sirius.kernel.commons.Strings;

import java.security.SecureRandom;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 11.01.14
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
@Table
public class User extends Entity {

    @RefType(Tenant.class)
    private One<Tenant> tenant;
    public static final String TENANT = "tenant";

    @Length(100)
    @NotNull
    @Unique
    private String loginName;
    public static final String LOGIN_NAME = "loginName";

    @Length(100)
    @NotNull
    private String salt;
    public static final String SALT = "salt";

    @Length(100)
    @NotNull
    private String passwordHash;

    private boolean active = true;

    @eMailAddress
    @Length(150)
    @NotNull
    private String email;

    private static final SecureRandom rnd = new SecureRandom();

    @Override
    protected void onBeforeSaveChecks() {
        if (Strings.isEmpty(salt)) {
            salt = Hashing.md5().hashString(String.valueOf(rnd.nextLong())).toString();
        }
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public One<Tenant> getTenant() {
        return tenant;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
