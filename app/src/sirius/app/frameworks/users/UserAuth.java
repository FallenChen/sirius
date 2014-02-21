package sirius.app.frameworks.users;

import com.google.common.collect.Sets;
import com.vaadin.ui.*;
import sirius.app.oma.OMA;
import sirius.app.vgui.Auth;
import sirius.app.vgui.GUI;
import sirius.kernel.commons.Strings;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 17:14
 * To change this template use File | Settings | File Templates.
 */
public class UserAuth implements Auth {

    private User user;
    private Tenant tenant;
    private Set<String> permissions = Sets.newHashSet();
    private Set<String> tenantPermissions = Sets.newHashSet();

    @Override
    public String getLoginName() {
        return user.getLoginName();
    }

    @Override
    public String getUserName() {
        return getLoginName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return Strings.isEmpty(permission) || permission.contains(permission);
    }

    @Override
    public boolean hasTenantPermission(String permission) {
        return Strings.isEmpty(permission) || tenantPermissions.contains(permission);
    }

    @Override
    public void logout() {
        user = null;
        tenant = null;
        permissions.clear();
        tenantPermissions.clear();
    }

    @Override
    public boolean isLoggedIn() {
        return user != null;
    }

    @Override
    public void setupLoginUI(final GUI gui, VerticalLayout content) {
        final TextField userName = new TextField("username");
        final PasswordField password = new PasswordField("password");
        Button login = new Button("Login");
        login.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                performLogin(userName.getValue(), password.getValue(), gui);
            }


        });
        userName.focus();
        content.addComponent(userName);
        content.addComponent(password);
        content.addComponent(login);
    }

    private void performLogin(String user, String password, GUI gui) {
        if (Strings.isEmpty(user)) {
            return;
        }
        if (Strings.isEmpty(password)) {
            return;
        }

        User u = OMA.select(User.class).eq(User.LOGIN_NAME, user).first();
        if (u != null) {
           // if (Strings.areEqual(Hashing.md5().hashString(u.getSalt() + password).toString(), u.getPasswordHash())) {
                this.user = u;
//                this.tenant = u.getTenant().getObject();
                gui.restartGUI();
                return;
           // }
        }
        Notification.show("§Ungültiger Benutzername oder Passwort!");
    }
}
