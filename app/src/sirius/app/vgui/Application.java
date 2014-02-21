package sirius.app.vgui;

import com.vaadin.ui.VerticalLayout;
import sirius.kernel.nls.NLS;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 16:19
 * To change this template use File | Settings | File Templates.
 */
public class Application {

    private GUI gui;

    public boolean isAuthorized(GUI gui) {
        return true;
    }

    protected GUI getGUI() {
        return gui;
    }

    public void setupUI(final GUI gui, VerticalLayout content) {
        this.gui = gui;
    }

    public String getTitle() {
        return NLS.get(getClass().getSimpleName() + ".title");
    }
}
