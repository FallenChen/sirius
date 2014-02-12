/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

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

    /**
     * Determines if mails sent via this configuration use a SENDER: header with the mail sender / senderName or not.
     *
     * @return <tt>true</tt> if a SENDER: header should be set, <tt>false</tt> otherwise
     */
    boolean isUseSenderAndEnvelopeFrom();
}
