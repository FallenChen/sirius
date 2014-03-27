/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.mails;


import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sun.mail.smtp.SMTPMessage;
import com.typesafe.config.Config;
import sirius.kernel.async.Async;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Part;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;
import sirius.web.http.MimeHelper;
import sirius.web.templates.Content;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Register(classes = {MailService.class})
public class MailService {

    public static final String X_BOUNCETOKEN = "X-Bouncetoken";

    protected static final Log MAIL = Log.get("mail");

    private static final String X_MAILER = "X-Mailer";
    private static final String MIXED = "mixed";
    private static final String TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=\"UTF-8\"";
    private static final String TEXT_PLAIN_CHARSET_UTF_8 = "text/plain; charset=\"UTF-8\"";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String MIME_VERSION_1_0 = "1.0";
    private static final String MIME_VERSION = "MIME-Version";
    private static final String ALTERNATIVE = "alternative";
    private static final String MAIL_USER = "mail.user";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_FROM = "mail.from";
    private static final String MAIL_SMTP_HOST = "mail.smtp.host";
    private static final String SMTP = "smtp";
    private static final String MAIL_SMTP_PORT = "mail.smtp.port";

    @ConfigValue("mail.smtp.host")
    private String smtpHost;
    @ConfigValue("mail.smtp.port")
    private int smtpPort;
    @ConfigValue("mail.smtp.user")
    private String smtpUser;
    @ConfigValue("mail.smtp.password")
    private String smtpPassword;
    @ConfigValue("mail.smtp.sender")
    private String smtpSender;
    @ConfigValue("mail.smtp.senderName")
    private String smtpSenderName;

    private final SMTPConfiguration DEFAULT_CONFIG = new DefaultSMTPConfig();

    @ConfigValue("mail.mailer")
    private String mailer;

    @Part
    private Content content;


    @Parts(MailLog.class)
    private Collection<MailLog> logs;

    public MailSender createEmail() {
        return new MailSender();
    }

    protected class DefaultSMTPConfig implements SMTPConfiguration {

        @Override
        public String getMailHost() {
            return smtpHost;
        }

        @Override
        public String getMailPort() {
            return String.valueOf(smtpPort);
        }

        @Override
        public String getMailUser() {
            return smtpUser;
        }

        @Override
        public String getMailPassword() {
            return smtpPassword;
        }

        @Override
        public String getMailSender() {
            return smtpSender;
        }

        @Override
        public String getMailSenderName() {
            return smtpSenderName;
        }

        @Override
        public boolean isUseSenderAndEnvelopeFrom() {
            return true;
        }
    }


    private class MailAuthenticator extends Authenticator {

        private SMTPConfiguration config;

        public MailAuthenticator(SMTPConfiguration config) {
            this.config = config;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(config.getMailUser(), config.getMailPassword());
        }
    }

    public Session getMailSession(SMTPConfiguration config) {
        Properties props = new Properties();
        props.put(MAIL_SMTP_PORT, Strings.isEmpty(config.getMailPort()) ? "25" : config.getMailPort());
        props.put(MAIL_SMTP_HOST, config.getMailHost());
        if (Strings.isFilled(config.getMailSender())) {
            props.put(MAIL_FROM, config.getMailSender());
        }
        props.put(MAIL_TRANSPORT_PROTOCOL, SMTP);
        Authenticator auth = new MailAuthenticator(config);
        if (Strings.isEmpty(config.getMailPassword())) {
            props.put(MAIL_SMTP_AUTH, Boolean.FALSE.toString());
            return Session.getInstance(props);
        } else {
            props.put(MAIL_USER, config.getMailUser());
            props.put(MAIL_SMTP_AUTH, Boolean.TRUE.toString());
            return Session.getInstance(props, auth);
        }
    }

    private MimeMultipart createContent(String textPart,
                                        String htmlPart,
                                        List<DataSource> attachments) throws Exception {
        MimeMultipart content = new MimeMultipart(ALTERNATIVE);
        MimeBodyPart text = new MimeBodyPart();
        MimeBodyPart html = new MimeBodyPart();
        text.setText(textPart, Charsets.UTF_8.name());
        text.setHeader(MIME_VERSION, MIME_VERSION_1_0);
        text.setHeader(CONTENT_TYPE, TEXT_PLAIN_CHARSET_UTF_8);
        content.addBodyPart(text);
        if (htmlPart != null) {
            htmlPart = Strings.replaceUmlautsToHtml(htmlPart);
            html.setText(htmlPart, Charsets.UTF_8.name());
            html.setHeader(MIME_VERSION, MIME_VERSION_1_0);
            html.setHeader(CONTENT_TYPE, TEXT_HTML_CHARSET_UTF_8);
            content.addBodyPart(html);
        }
        if (!attachments.isEmpty()) {
            for (DataSource attachment : attachments) {
                // Generate a new root-multipart which contains the mail-content
                // as alternative-content as well as the attachments.
                MimeMultipart mixed = new MimeMultipart(MIXED);
                MimeBodyPart contentPart = new MimeBodyPart();
                contentPart.setContent(content);
                mixed.addBodyPart(contentPart);
                content = mixed;
                // Filter null values since var-args are tricky...
                if (attachment != null) {
                    MimeBodyPart part = new MimeBodyPart();
                    part.setDisposition(javax.mail.Part.ATTACHMENT);
                    part.setDataHandler(new DataHandler(attachment));
                    part.setFileName(attachment.getName());
                    content.addBodyPart(part);
                }
            }
        }
        return content;
    }

    public class MailSender {

        private String senderEmail;
        private String senderName;
        private String receiverEmail;
        private String receiverName;
        private String subject;
        private String mailExtension;
        private Context context;
        private boolean includeHTMLPart = true;
        private String text;
        private String html;
        private List<DataSource> attachments = Lists.newArrayList();
        private String bounceToken;
        private String lang;

        public MailSender setLang(String... langs) {
            if (langs == null) {
                return this;
            }
            for (String lang : langs) {
                if (Strings.isFilled(lang)) {
                    this.lang = lang;
                    return this;
                }
            }
            return this;
        }

        public MailSender fromEmail(String senderEmail) {
            this.senderEmail = senderEmail;
            return this;
        }

        public MailSender fromName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public MailSender toEmail(String receiverEmail) {
            this.receiverEmail = receiverEmail;
            return this;
        }

        public MailSender toName(String receiverName) {
            this.receiverName = receiverName;
            return this;
        }

        public MailSender subject(String subject) {
            this.subject = subject;
            return this;
        }

        public MailSender useMailTemplate(String mailExtension, @Nonnull Context context) {
            this.mailExtension = mailExtension;
            this.context = context;
            return this;
        }

        public MailSender includeHTMLPart(boolean includeHTMLPart) {
            this.includeHTMLPart = includeHTMLPart;
            return this;
        }

        public MailSender textContent(String text) {
            this.text = text;
            return this;
        }

        public MailSender htmlContent(String html) {
            this.html = html;
            return this;
        }

        public MailSender addAttachment(DataSource attachment) {
            attachments.add(attachment);
            return this;
        }

        public MailSender addAttachments(DataSource... attachment) {
            if (attachment != null) {
                attachments.addAll(Arrays.asList(attachment));
            }
            return this;
        }

        public MailSender addAttachments(List<DataSource> attachments) {
            if (attachments != null) {
                this.attachments.addAll(attachments);
            }
            return this;
        }

        public MailSender setBounceToken(String token) {
            this.bounceToken = token;
            return this;
        }

        public void send() {
            SMTPConfiguration config = new DefaultSMTPConfig();
            String tmpLang = NLS.getCurrentLang();
            try {
                try {
                    if (lang != null) {
                        CallContext.getCurrent().setLang(lang);
                    }
                    fill();
                    sanitize();
                    check();
                    SendMailTask task = new SendMailTask(this, config);
                    Async.executor("email").fork(task).execute();
                } finally {
                    CallContext.getCurrent().setLang(tmpLang);
                }
            } catch (HandledException e) {
                throw e;
            } catch (Throwable e) {
                throw Exceptions.handle()
                                .withSystemErrorMessage(
                                        "Cannot send mail to '%s (%s)' from '%s (%s)' with subject '%s': %s (%s)",
                                        receiverEmail,
                                        receiverName,
                                        senderEmail,
                                        senderName,
                                        subject)
                                .to(MAIL)
                                .error(e)
                                .handle();
            }
        }

        private void check() {
            try {
                if (Strings.isFilled(receiverName)) {
                    new InternetAddress(receiverEmail, receiverName).validate();
                } else {
                    new InternetAddress(receiverEmail).validate();
                }
            } catch (Throwable e) {
                throw Exceptions.handle()
                                .to(MAIL)
                                .error(e)
                                .withNLSKey("MailServiceBean.invalidReceiver")
                                .set("address", receiverEmail)
                                .set("name", receiverName)
                                .handle();
            }

            try {
                if (Strings.isFilled(senderEmail)) {
                    if (Strings.isFilled(senderName)) {
                        new InternetAddress(senderEmail, senderName).validate();
                    } else {
                        new InternetAddress(senderEmail).validate();
                    }
                }
            } catch (Throwable e) {
                throw Exceptions.handle()
                                .to(MAIL)
                                .error(e)
                                .withNLSKey("MailServiceBean.invalidSender")
                                .set("address", senderEmail)
                                .set("name", senderName)
                                .handle();
            }
        }

        private void sanitize() {
            if (Strings.isFilled(senderEmail)) {
                senderEmail = senderEmail.replaceAll("\\s", "");
            }
            if (Strings.isFilled(senderName)) {
                senderName = senderName.trim();
            }
            if (Strings.isFilled(receiverEmail)) {
                receiverEmail = receiverEmail.replaceAll("\\s", "");
            }
            if (Strings.isFilled(receiverName)) {
                receiverName = receiverName.trim();
            }
            if (!includeHTMLPart) {
                html = null;
            }
        }

        private void fill() {
            if (Strings.isEmpty(mailExtension)) {
                return;
            }
            Extension ex = Extensions.getExtension("mail.templates", mailExtension);
            if (ex == null) {
                throw Exceptions.handle()
                                .withSystemErrorMessage(
                                        "Unknown mail extension: %s. Cannot send mail from: '%s' to '%s'",
                                        mailExtension,
                                        senderEmail,
                                        receiverEmail)
                                .handle();
            }
            context.put("template", mailExtension);
            try {
                subject(content.generator().direct(ex.get("subject").asString()).applyContext(context).generate());
                textContent(content.generator()
                                   .useTemplate(ex.get("text").asString())
                                   .applyContext(context)
                                   .generate());
                htmlContent(null);
                if (ex.get("html").isFilled()) {
                    try {
                        htmlContent(content.generator()
                                           .useTemplate(ex.get("html").asString())
                                           .applyContext(context)
                                           .generate());
                    } catch (Throwable e) {
                        Exceptions.handle()
                                  .to(MAIL)
                                  .error(e)
                                  .withSystemErrorMessage(
                                          "Cannot generate HTML content using template %s (%s) when sending a mail from '%s' to '%s': %s (%s)",
                                          mailExtension,
                                          ex.get("html").asString(),
                                          senderEmail,
                                          receiverEmail)
                                  .handle();
                    }
                }

                for (Config attachmentConfig : ex.getConfigs("attachments")) {
                    String template = attachmentConfig.getString("template");
                    try {
                        Content.Generator attachment = content.generator();
                        if (attachmentConfig.hasPath("encoding")) {
                            attachment.encoding(attachmentConfig.getString("encoding"));
                        }
                        attachment.useTemplate(template);
                        attachment.applyContext(context);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        attachment.generateTo(out);
                        out.flush();
                        String fileName = attachmentConfig.getString("id");
                        if (attachmentConfig.hasPath("fileName")) {
                            fileName = content.generator()
                                              .direct(attachmentConfig.getString("fileName"))
                                              .applyContext(context)
                                              .generate();
                        } else {
                            int idx = fileName.lastIndexOf("-");
                            if (idx >= 0) {
                                fileName = fileName.substring(0, idx) + "." + fileName.substring(idx + 1);
                            }
                        }

                        String mimeType = MimeHelper.guessMimeType(fileName);
                        addAttachment(new Attachment(fileName, mimeType, out.toByteArray()));
                    } catch (Throwable t) {
                        Exceptions.handle()
                                  .to(MAIL)
                                  .error(t)
                                  .withSystemErrorMessage(
                                          "Cannot generate attachment using template %s (%s) when sending a mail from '%s' to '%s': %s (%s)",
                                          mailExtension,
                                          template,
                                          senderEmail,
                                          receiverEmail)
                                  .handle();
                    }
                }
            } catch (HandledException e) {
                throw e;
            } catch (Throwable e) {
                throw Exceptions.handle()
                                .withSystemErrorMessage(
                                        "Cannot send mail to '%s (%s)' from '%s (%s)' with subject '%s': %s (%s)",
                                        receiverEmail,
                                        receiverName,
                                        senderEmail,
                                        senderName,
                                        subject)
                                .to(MAIL)
                                .error(e)
                                .handle();
            }
        }

        public MailSender from(String senderEmail, String senderName) {
            return fromEmail(senderEmail).fromName(senderName);
        }

        public MailSender to(String receiverEmail, String receiverName) {
            return toEmail(receiverEmail).toName(receiverName);
        }

    }

    private class SendMailTask implements Runnable {

        private MailSender mail;
        private SMTPConfiguration config;

        public SendMailTask(MailSender mail, SMTPConfiguration config) {
            this.mail = mail;
            this.config = config;
        }

        @Override
        public void run() {
            boolean success = false;
            String messageId = null;
            String technicalSender = config.getMailSender();
            String technicalSenderName = config.getMailSenderName();
            if (Strings.isEmpty(technicalSender)) {
                technicalSender = DEFAULT_CONFIG.getMailSender();
                technicalSenderName = DEFAULT_CONFIG.getMailSenderName();
            }
            try {
                try {
                    MAIL.FINE("Sending eMail: " + mail.subject + " to: " + mail.receiverEmail);

                    Session session = getMailSession(config);
                    Transport transport = getSMTPTransport(session, config);
                    try {
                        try {
                            SMTPMessage msg = new SMTPMessage(session);
                            msg.setSubject(mail.subject);
                            msg.setRecipients(Message.RecipientType.TO,
                                              new InternetAddress[]{new InternetAddress(mail.receiverEmail,
                                                                                        mail.receiverName)}
                            );
                            if (Strings.isFilled(mail.senderEmail)) {
                                if (config.isUseSenderAndEnvelopeFrom()) {
                                    msg.setSender(new InternetAddress(technicalSender, technicalSenderName));
                                }
                                msg.setFrom(new InternetAddress(mail.senderEmail, mail.senderName));
                            } else {
                                msg.setFrom(new InternetAddress(technicalSender, technicalSenderName));
                            }
                            if (Strings.isFilled(mail.html) || !mail.attachments.isEmpty()) {
                                MimeMultipart content = createContent(mail.text, mail.html, mail.attachments);
                                msg.setContent(content);
                                msg.setHeader(CONTENT_TYPE, content.getContentType());
                            } else {
                                if (mail.text != null) {
                                    msg.setText(mail.text);
                                } else {
                                    msg.setText("");
                                }
                            }
                            if (config.isUseSenderAndEnvelopeFrom()) {
                                msg.setEnvelopeFrom(Strings.isFilled(config.getMailSender()) ? config.getMailSender() : DEFAULT_CONFIG
                                        .getMailSender());
                            }
                            msg.setHeader(MIME_VERSION, MIME_VERSION_1_0);
                            if (Strings.isFilled(mail.bounceToken)) {
                                msg.setHeader(X_BOUNCETOKEN, mail.bounceToken);
                            }
                            msg.setHeader(X_MAILER, mailer);
                            msg.setSentDate(new Date());
                            transport.sendMessage(msg, msg.getAllRecipients());
                            messageId = msg.getMessageID();
                            success = true;
                        } catch (Throwable e) {
                            throw Exceptions.handle()
                                            .withSystemErrorMessage(
                                                    "Cannot send mail to %s from %s with subject '%s': %s (%s)",
                                                    mail.receiverEmail,
                                                    mail.senderEmail,
                                                    mail.subject)
                                            .to(MAIL)
                                            .error(e)
                                            .handle();
                        }
                    } finally {
                        transport.close();
                    }
                } catch (HandledException e) {
                    throw e;
                } catch (Throwable e) {
                    // If we have no host to use as sender - don't complain too much...
                    Exceptions.ErrorHandler handler = Strings.isFilled(config.getMailHost()) ? Exceptions.handle() : Exceptions
                            .createHandled();
                    throw handler.withSystemErrorMessage(
                            "Invalid mail configuration: %s (Host: %s, Port: %s, User: %s, Password used: %s)",
                            e.getMessage(),
                            config.getMailHost(),
                            config.getMailPort(),
                            config.getMailUser(),
                            Strings.isFilled(config.getMailPassword())).to(MAIL).error(e).handle();
                }
            } finally {
                if (logs.isEmpty()) {
                    if (!success) {
                        MAIL.WARN("FAILED to send mail from: '%s' to '%s' with subject: '%s'",
                                  Strings.isEmpty(mail.senderEmail) ? technicalSender : mail.senderEmail,
                                  mail.receiverEmail,
                                  mail.subject);
                    } else {
                        MAIL.FINE("Sent mail from: '%s' to '%s' with subject: '%s'",
                                  Strings.isEmpty(mail.senderEmail) ? technicalSender : mail.senderEmail,
                                  mail.receiverEmail,
                                  mail.subject);
                    }
                } else {
                    for (MailLog log : logs) {
                        try {
                            log.logSentMail(success,
                                            messageId,
                                            Strings.isEmpty(mail.senderEmail) ? technicalSender : mail.senderEmail,
                                            Strings.isEmpty(mail.senderEmail) ? technicalSenderName : mail.senderName,
                                            mail.receiverEmail,
                                            mail.receiverName,
                                            mail.subject,
                                            mail.text,
                                            mail.html);
                        } catch (Exception e) {
                            Exceptions.handle(MAIL, e);
                        }
                    }
                }
            }
        }
    }

    public Transport getSMTPTransport(Session session, SMTPConfiguration config) {
        try {
            Transport transport = session.getTransport(SMTP);
            transport.connect(config.getMailHost(), config.getMailUser(), null);
            return transport;
        } catch (Exception e) {
            throw Exceptions.handle()
                            .withSystemErrorMessage(
                                    "Invalid mail configuration: %s (Host: %s, Port: %s, User: %s, Password used: %s)",
                                    e.getMessage(),
                                    config.getMailHost(),
                                    config.getMailPort(),
                                    config.getMailUser(),
                                    Strings.isFilled(config.getMailPassword()))
                            .to(MAIL)
                            .error(e)
                            .handle();
        }
    }

}
