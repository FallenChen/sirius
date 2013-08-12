package sirius.web.controller;

import sirius.kernel.health.HandledException;
import sirius.web.http.WebContext;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 14:21
 * To change this template use File | Settings | File Templates.
 */
public interface Controller {

    void onError(WebContext ctx, HandledException error);

}
