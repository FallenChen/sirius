package sirius.web.http;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.01.14
 * Time: 15:37
 * To change this template use File | Settings | File Templates.
 */
public interface ActiveHTTPConnection {
    int getNumKeepAlive();

    String getURL();

    String getConnectedSince();

    String getBytesIn();

    String getBytesOut();

    String getUplink();

    String getDownlink();

    String getRemoteAddress();
}
