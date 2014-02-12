package sirius.web.mails;

import sirius.kernel.commons.Context;

import javax.activation.DataSource;
import java.util.List;

/**
 * Used to send and receives mails.
 */
public interface MailService {

    /**
     * Fluent interface for sending emails.
     */
    public static interface MailSender {

        /**
         * Sets the eMail-Address from which the mail is sent.
         */
        MailSender fromEmail(String senderEmail);

        /**
         * Sets the Name from which the mail is sent.
         */
        MailSender fromName(String senderName);

        /**
         * Boilerplate for fromEmail, fromName
         */
        MailSender from(String senderEmail, String senderName);

        /**
         * Sets the eMail-Address to which the mail is sent.
         */
        MailSender toEmail(String receiverEmail);

        /**
         * Sets the name to which the mail is sent.
         */
        MailSender toName(String receiverName);

        /**
         * Boilerplate for toEmail, toName
         */
        MailSender to(String receiverEmail, String receiverName);

        /**
         * Sets the subject line.
         */
        MailSender subject(String subject);

        /**
         * Utilizes a mail-extension to generate the mail.
         */
        MailSender useMailTemplate(String mailExtension);

        /**
         * Boilerplate for useMaiLExtension, applyContext.
         */
        MailSender useMailTemplate(String mailExtension, Context ctx);

        /**
         * Sets the context used to fill the mail-extension.
         */
        MailSender applyContext(Context context);

        /**
         * Decides whether the HTML part should be included.
         */
        MailSender includeHTMLPart(boolean includeHTMLPart);

        /**
         * Sets the text content for this mail.
         */
        MailSender textContent(String text);

        /**
         * Sets the html content for this mail.
         */
        MailSender htmlContent(String html);

        /**
         * Adds an attachment to the mail.
         */
        MailSender addAttachment(DataSource attachment);

        /**
         * Adds the given attachments to the mail.
         */
        MailSender addAttachments(DataSource... attachment);

        /**
         * Adds the given attachments to the mail.
         */
        MailSender addAttachments(List<DataSource> attachments);

        /**
         * Sets the bounce-token. Can be used to get notified if the mail is
         * bounced.
         */
        MailSender setBounceToken(String token);

        /**
         * Sets the Language used for the template
         */
        MailSender setLang(String... langs);

        /**
         * Finally sends the mail.
         */
        void send();

    }

    /**
     * Creates a {@link MailSender}.
     */
    MailSender createEmail();

}
