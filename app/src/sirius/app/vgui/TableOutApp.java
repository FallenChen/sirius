package sirius.app.vgui;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 16:50
 * To change this template use File | Settings | File Templates.
 */
public class TableOutApp extends Application {
    @Override
    public void setupUI(GUI gui, VerticalLayout content) {
        super.setupUI(gui, content);
        final TextField test = new TextField("test");
        final Table table = new Table("This is my Table");
        content.addShortcutListener(new ShortcutListener("Test", ShortcutAction.KeyCode.ENTER, new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (target == test) {
                    table.focus();
                }
                if (target == table) {
                    test.focus();
                }
            }
        });
        content.addComponent(test);
/* Create the table with a caption. */

/* Define the names and data types of columns.
 * The "default value" parameter is meaningless here. */

table.setContainerDataSource(new OMAContainer());
        table.setSelectable(true);
        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                table.setValue(event.getItem());
                test.focus();
            }
        });
        table.addShortcutListener(new ShortcutListener("Test1", ShortcutAction.KeyCode.ENTER, new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (target == table) {
                    test.focus();
                }
            }
        });
        table.addShortcutListener(new ShortcutListener("Test2", ShortcutAction.KeyCode.DELETE,
                                                       new int[0]) {
            @Override
            public void handleAction(Object sender, Object target) {
                table.removeItem(table.getValue());
                table.setValue(1);
            }
        });

        table.focus();

/* Add a few items in the table. */
        table.addItem(new Object[] {
                "Nicolaus","Copernicus",new Integer(1473)}, new Integer(1));
        table.addItem(new Object[] {
                "Tycho",   "Brahe",     new Integer(1546)}, new Integer(2));
        table.addItem(new Object[] {
                "Giordano","Bruno",     new Integer(1548)}, new Integer(3));
        table.addItem(new Object[] {
                "Galileo", "Galilei",   new Integer(1564)}, new Integer(4));
        table.addItem(new Object[] {
                "Johannes","Kepler",    new Integer(1571)}, new Integer(5));
        table.addItem(new Object[] {
                "Isaac",   "Newton",    new Integer(1643)}, new Integer(6));
        table.setSizeFull();

        content.addComponent(table);
    }
}
