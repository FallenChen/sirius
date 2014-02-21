package sirius.app.vgui;

import com.google.common.collect.Lists;
import com.vaadin.annotations.Push;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.*;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.health.Exceptions;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 15:19
 * To change this template use File | Settings | File Templates.
 */
@Push(PushMode.MANUAL)
public class GUI extends UI {

    private Auth auth;
    private List<Application> appStack = Lists.newArrayList();
    private VerticalLayout content;
    private VerticalLayout mainLayout;

    public Auth getAuth() {
        return auth;
    }

    @ConfigValue("vgui.authClass")
    private static String authClass;

    @Override
    protected void init(VaadinRequest request) {
        try {
            auth = (Auth) Class.forName(authClass).newInstance();
        } catch (Throwable e) {
            Notification.show(Exceptions.handle(e).getMessage(), Notification.Type.ERROR_MESSAGE);
            return;
        }

        mainLayout = new VerticalLayout();
        setContent(mainLayout);

        restartGUI();

    }

    public void returnFrom(Application app, ReturnHandler.ReturnType type) {

    }

    public void restartGUI() {
        if (!auth.isLoggedIn()) {
            mainLayout.removeAllComponents();
            auth.setupLoginUI(this, mainLayout);
        } else {
            mainLayout.removeAllComponents();
            mainLayout.setSizeFull();
            HorizontalLayout header = new HorizontalLayout();
            content = new VerticalLayout();
            final TextField commandBox = new TextField("");
            header.addComponent(commandBox);
            header.setSizeUndefined();
            mainLayout.addComponent(header);
            content.setSizeFull();
            mainLayout.addComponent(content);
            setContent(mainLayout);
            commandBox.addTextChangeListener(new FieldEvents.TextChangeListener() {
                @Override
                public void textChange(FieldEvents.TextChangeEvent event) {
                    Notification.show(event.getText(), "XXXXX", Notification.Type.HUMANIZED_MESSAGE);
                    System.out.println(event.getText());
                    commandBox.setValue("");
                }
            });
            mainLayout.addShortcutListener(new AbstractField.FocusShortcut(commandBox, ShortcutAction.KeyCode.F1));
            runApp(new TableOutApp(), null);

        }
    }

    public void logout() {
        auth.logout();
        restartGUI();
    }

    public void runApp(Application app, ReturnHandler returnHandler) {
        if (app.isAuthorized(this)) {
            content.removeAllComponents();
            app.setupUI(this, content);
            appStack.add(app);
        } else {
            Notification.show("§Access to: " + app.getTitle() + " not allowed");
        }
    }

}
