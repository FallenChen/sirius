package sirius.app.vgui;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 19.01.14
 * Time: 16:38
 * To change this template use File | Settings | File Templates.
 */
public interface ReturnHandler<A extends Application> {

    enum ReturnType {
        OK,
        CANCEL,
        CLOSE
    }

    void onComplete(A sender, ReturnType type);
}
