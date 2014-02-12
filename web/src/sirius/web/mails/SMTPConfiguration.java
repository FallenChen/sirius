package sirius.web.mails;

/**
 * Represents a configuration for using a SMTP server.
 */
public interface SMTPConfiguration {
    /**
     * Contains the hostname of the server.
     */
    String getMailHost();

    /**
     * Contains the portname to use.
     */
    String getMailPort();

    /**
     * Contains the username to login
     */
    String getMailUser();

    /**
     * Contains the passwort to login.
     */
    String getMailPassword();

    /**
     * Contains the default sender address
     */
    String getMailSender();

    /**
     * Contains the default sender name
     */
    String getMailSenderName();
}
