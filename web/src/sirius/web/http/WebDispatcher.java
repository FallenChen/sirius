package sirius.web.http;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 20:42
 * To change this template use File | Settings | File Templates.
 */
public interface WebDispatcher {

    int getPriority();

    boolean dispatch(WebContext ctx) throws Exception;

}
