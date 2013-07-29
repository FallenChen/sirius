package sirius.web.health;

import sirius.kernel.di.annotations.Register;
import sirius.web.http.WebContext;
import sirius.web.http.controller.BasicController;
import sirius.web.http.controller.Controller;
import sirius.web.http.controller.Routed;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 16:25
 * To change this template use File | Settings | File Templates.
 */
@Register(classes = Controller.class)
public class SystemController extends BasicController {

    @Routed("/system/console")
    public void console(WebContext ctx) {
        ctx.respondWith().cached().template("/view/system/console.html");
    }

}
