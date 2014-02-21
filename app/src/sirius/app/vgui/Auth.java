package sirius.app.vgui;

import com.vaadin.ui.VerticalLayout;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 17:06
 * To change this template use File | Settings | File Templates.
 */
public interface Auth {

    String getLoginName();

    String getUserName();

    boolean hasPermission(String permission);

    boolean hasTenantPermission(String permission);

    void logout();

    boolean isLoggedIn();

    void setupLoginUI(GUI gui, VerticalLayout content);

    //TODO
    //Adaptable getUser();

}
