package sirius.web.mails;

import javax.mail.internet.MimeMessage;import java.lang.String;

/**
 * Instances will be notified if an email bounces.
 */
public interface BounceHandler {
    /**
     * Notified if a mail bounces.
     */
    void handleBounce(String bounceToken, MimeMessage msg);
}
