package sirius.web.mails;

import com.google.common.base.Charsets;
import com.scireum.common.BusinessException;
import com.scireum.common.Log;
import com.scireum.common.Tools;
import com.scireum.common.http.MimeHelper;
import com.scireum.common.nls.NLS.Lang;
import com.scireum.ocm.BIZ;
import com.scireum.ocm.content.ArtifactManager;
import com.scireum.ocm.health.Syslog;
import com.scireum.ocm.incidents.Incidents;
import com.scireum.ocm.plugins.Plugins;
import com.scireum.ocm.plugins.xml.Extension;
import com.scireum.ocm.plugins.xml.Section;
import com.scireum.ocm.tasks.Task;
import com.scireum.ocm.tasks.TaskMonitor;
import com.scireum.ocm.tasks.Tasks;
import com.scireum.ocm.user.*;
import com.scireum.ocm.web.servlet.content.ContentGenerator;
import com.sun.mail.smtp.SMTPMessage;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.*;

@Register(classes = {MailService.class})
public class MailServiceBean implements MailService {

    private static final String X_MAILER = "X-Mailer";
    public static final String X_BOUNCETOKEN = "X-Bouncetoken";
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

    }

    private final SMTPConfiguration DEFAULT_CONFIG = new DefaultSMTPConfig();

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
                // as alternative-content as well as the attachements.
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

    private class MailSenderImpl implements MailSender {

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
        private List<DataSource> attachments = new ArrayList<DataSource>();
        private String bounceToken;
        private String lang;

        @Override
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

        @Override
        public MailSender fromEmail(String senderEmail) {
            this.senderEmail = senderEmail;
            return this;
        }

        @Override
        public MailSender fromName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        @Override
        public MailSender toEmail(String receiverEmail) {
            this.receiverEmail = receiverEmail;
            return this;
        }

        @Override
        public MailSender toName(String receiverName) {
            this.receiverName = receiverName;
            return this;
        }

        @Override
        public MailSender subject(String subject) {
            this.subject = subject;
            return this;
        }

        @Override
        public MailSender useMailTemplate(String mailExtension) {
            this.mailExtension = mailExtension;
            return this;
        }

        @Override
        public MailSender applyContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        public MailSender includeHTMLPart(boolean includeHTMLPart) {
            this.includeHTMLPart = includeHTMLPart;
            return this;
        }

        @Override
        public MailSender textContent(String text) {
            this.text = text;
            return this;
        }

        @Override
        public MailSender htmlContent(String html) {
            this.html = html;
            return this;
        }

        @Override
        public MailSender addAttachment(DataSource attachment) {
            attachments.add(attachment);
            return this;
        }

        @Override
        public MailSender addAttachments(DataSource... attachment) {
            if (attachment != null) {
                attachments.addAll(Arrays.asList(attachment));
            }
            return this;
        }

        @Override
        public MailSender addAttachments(List<DataSource> attachments) {
            if (attachments != null) {
                this.attachments.addAll(attachments);
            }
            return this;
        }

        @Override
        public MailSender setBounceToken(String token) {
            this.bounceToken = token;
            return this;
        }

        @Override
        public void send() {
            SMTPConfiguration config = new DefaultSMTPConfig();
            try {
                String tmpLang = NLS.getCurrentLang();
                try {
                    try {
                        if (frontendUser != null) {
                            Clients.setCurrentUser(frontendUser);
                        }
                        try {
                            if (backendUser != null) {
                                Users.setCurrentUser(backendUser);
                            }
                            try {
                                if (lang != null) {
                                    NLS.setCurrentLang(lang);
                                }
                                fill();
                                sanitize();
                                check();
                                SendMailTask task = new SendMailTask(this, config);
                                Tasks.submit("EMAIL", task, null);
                            } finally {
                                NLS.setCurrentLang(tmpLang);
                            }
                        } finally {
                            if (backendUser != null) {
                                Users.setCurrentUser(tmpUser);
                            }
                        }
                    } finally {
                        if (frontendUser != null) {
                            Clients.setCurrentUser(tmpClient);
                        }
                    }
                } finally {
                    if (space != null) {
                        Spaces.setSpace(tmpSpace);
                    }
                }

            } catch (BusinessException e) {
                throw e;
            } catch (Throwable e) {
                throw Incidents.named("scireum.biz.InvalidMailConfiguration")
                               .withException(e)
                               .set("host", config.getMailHost())
                               .set("user", config.getMailUser())
                               .set("hasPassword", String.valueOf(!Tools.emptyString(config.getMailPassword())))
                               .handle();
            }
        }

        private void check() {
            try {
                if (Tools.notEmpty(receiverName)) {
                    new InternetAddress(receiverEmail, receiverName).validate();
                } else {
                    new InternetAddress(receiverEmail).validate();
                }
                if (Tools.notEmpty(senderEmail)) {
                    if (Tools.notEmpty(senderName)) {
                        new InternetAddress(senderEmail, senderName).validate();
                    } else {
                        new InternetAddress(senderEmail).validate();
                    }
                }
            } catch (Throwable e) {
                throw Incidents.named("scireum.biz.CannotSendMail")
                               .severe()
                               .withException(e)
                               .set("subject", subject)
                               .set("receiver", receiverEmail)
                               .set("sender", senderEmail)
                               .handle();
            }
        }

        private void sanitize() {
            if (Tools.notEmpty(senderEmail)) {
                senderEmail = senderEmail.replaceAll("\\s", "");
            }
            if (Tools.notEmpty(senderName)) {
                senderName = senderName.trim();
            }
            if (Tools.notEmpty(receiverEmail)) {
                receiverEmail = receiverEmail.replaceAll("\\s", "");
            }
            if (Tools.notEmpty(receiverName)) {
                receiverName = receiverName.trim();
            }
            if (!includeHTMLPart) {
                html = null;
            }
        }

        private void fill() {
            if (Tools.emptyString(mailExtension)) {
                return;
            }
            Extension ex = Plugins.getExtension(mailExtension);
            if (ex == null) {
                throw Incidents.named("scireum.biz.UnknownMailExtension").set("extensionId", mailExtension).handle();
            }
            if (context == null) {
                context = ContentGenerator.createInitializedContext();
            }
            context.put("template", mailExtension);
            try {
                subject(ContentGenerator.getInstance()
                                        .generateDirectString(mailExtension + ".subject",
                                                              ex.getValue("subject"),
                                                              context));
                textContent(ContentGenerator.getInstance().generateString(ex.getValue("text"), context));
                htmlContent(null);
                if (Tools.notEmpty(ex.getValue("html"))) {
                    try {
                        htmlContent(ContentGenerator.getInstance().generateString(ex.getValue("html"), context));
                    } catch (Throwable e) {
                        throw Incidents.named("scireum.biz.CannotCreateHTMLPart")
                                       .withException(e)
                                       .set("extensionId", mailExtension)
                                       .set("content", ex.getValue("html"))
                                       .handle();
                    }
                }
                for (Section s : ex.getSection()) {
                    if ("attachment".equals(s.getName())) {
                        if (Clients.hasCustomerFeature(s.getValue("customerFeature")) && Spaces.hasFeature(s.getValue(
                                "spaceFeature")) && Users.hasFeature(s.getValue("tenantFeature"))) {
                            try {
                                String mimeType = MimeHelper.guessMimeType(s.getValue("artifact"));

                                if (s.getValue("direct") != null && Boolean.parseBoolean(s.getValue("direct"))) {
                                    addAttachment(new Attachment(s.getValue("fileName"),
                                                                 mimeType,
                                                                 Tools.readIntoBuffer(am.getArtifact(s.getValue(
                                                                         "artifact"), null))));
                                } else {
                                    if (Tools.notEmpty(s.getValue(ContentGenerator.ENCODING))) {
                                        context.put(ContentGenerator.ENCODING, s.getValue(ContentGenerator.ENCODING));
                                    } else {
                                        context.remove(ContentGenerator.ENCODING);
                                    }
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    ContentGenerator.getInstance().generate(s.getValue("artifact"), out, context);
                                    out.flush();
                                    addAttachment(new Attachment(ContentGenerator.getInstance()
                                                                                 .generateDirectString("",
                                                                                                       s.getValue(
                                                                                                               "fileName"),
                                                                                                       context),
                                                                 mimeType,
                                                                 out.toByteArray()));
                                }
                            } catch (Throwable e) {
                                Incidents.named("scireum.biz.CannotCreateAttachement")
                                         .severe()
                                         .set("subject", subject)
                                         .set("receiver", receiverEmail)
                                         .set("sender", senderEmail)
                                         .withException(e)
                                         .set("extensionId", mailExtension)
                                         .set("artifact", s.getValue("artifact"))
                                         .handle();
                            }
                        }
                    }
                }
            } catch (BusinessException e) {
                throw e;
            } catch (IOException e) {
                throw Incidents.named("scireum.biz.CannotCreateEMail")
                               .withException(e)
                               .set("extensionId", mailExtension)
                               .handle();
            }
        }

        @Override
        public MailSender from(String senderEmail, String senderName) {
            return fromEmail(senderEmail).fromName(senderName);
        }

        @Override
        public MailSender to(String receiverEmail, String recieverName) {
            return toEmail(receiverEmail).toName(recieverName);
        }

        @Override
        public MailSender useMailTemplate(String mailExtension, Context ctx) {
            return useMailTemplate(mailExtension).applyContext(ctx);
        }

    }

    private class SendMailTask implements Task {

        private static final long serialVersionUID = 1259628374286750953L;
        private MailSenderImpl mail;
        private SMTPConfiguration config;

        public SendMailTask(MailSenderImpl mail, SMTPConfiguration config) {
            this.mail = mail;
            this.config = config;
        }

        @Override
        public void execute(TaskMonitor monitor) throws Exception {
            boolean success = false;
            String messageId = null;
            String technicalSender = config.getMailSender();
            String technicalSenderName = config.getMailSenderName();
            if (Tools.emptyString(technicalSender)) {
                technicalSender = DEFAULT_CONFIG.getMailSender();
                technicalSenderName = DEFAULT_CONFIG.getMailSenderName();
            }
            try {
                try {
                    BIZ.LOG.FINE("Sending eMail: " + mail.subject + " to: " + mail.receiverEmail);

                    Session session = getMailSession(config);
                    Transport transport = getSMTPTransport(session, config);
                    try {
                        try {
                            SMTPMessage msg = new SMTPMessage(session);
                            msg.setSubject(mail.subject);
                            msg.setRecipients(Message.RecipientType.TO,
                                              new InternetAddress[]{new InternetAddress(mail.receiverEmail,
                                                                                        mail.receiverName)});
                            if (Tools.notEmpty(mail.senderEmail)) {
                                msg.setSender(new InternetAddress(technicalSender, technicalSenderName));
                                msg.setFrom(new InternetAddress(mail.senderEmail, mail.senderName));
                            } else {
                                msg.setFrom(new InternetAddress(technicalSender, technicalSenderName));
                            }
                            if (Tools.notEmpty(mail.html) || !mail.attachments.isEmpty()) {
                                MimeMultipart content = createContent(mail.text, mail.html, mail.attachments);
                                msg.setContent(content);
                                msg.setHeader(CONTENT_TYPE, content.getContentType());
                            } else {
                                msg.setText(mail.text);
                            }
                            msg.setEnvelopeFrom(Tools.notEmpty(config.getMailSender()) ? config.getMailSender() : DEFAULT_CONFIG
                                    .getMailSender());
                            msg.setHeader(MIME_VERSION, MIME_VERSION_1_0);
                            if (Tools.notEmpty(mail.bounceToken)) {
                                msg.setHeader(X_BOUNCETOKEN, mail.bounceToken);
                            }
                            msg.setHeader(X_MAILER, "scireum Mail-Subsystem");
                            msg.setSentDate(new Date());
                            transport.sendMessage(msg, msg.getAllRecipients());
                            messageId = msg.getMessageID();
                            success = true;
                        } catch (Throwable e) {
                            throw Incidents.named("scireum.biz.CannotSendMail")
                                           .severe()
                                           .withException(e)
                                           .set("subject", mail.subject)
                                           .set("receiver", mail.receiverEmail)
                                           .set("sender", mail.senderEmail)
                                           .handle();
                        }
                    } finally {
                        transport.close();
                    }
                } catch (BusinessException e) {
                    throw e;
                } catch (Throwable e) {
                    throw Incidents.named("scireum.biz.InvalidMailConfiguration")
                                   .withException(e)
                                   .set("host", config.getMailHost())
                                   .set("user", config.getMailUser())
                                   .set("hasPassword", String.valueOf(!Tools.emptyString(config.getMailPassword())))
                                   .handle();
                }

            } finally {
                Syslog.logMail(success,
                               messageId,
                               Tools.emptyString(mail.senderEmail) ? technicalSender : mail.senderEmail,
                               Tools.emptyString(mail.senderEmail) ? technicalSenderName : mail.senderName,
                               mail.receiverEmail,
                               mail.receiverName,
                               mail.subject,
                               mail.text,
                               mail.html);
            }
        }

        @Override
        public String getDescription() {
            return NLS.fmtr("scireum.biz.SendMailsTask").set("receiver", mail.receiverEmail).format();
        }

        @Override
        public Log getLog() {
            return BIZ.LOG;
        }

    }

    public Transport getSMTPTransport(Session session, SMTPConfiguration config) {
        try {
            Transport transport = session.getTransport(SMTP);
            transport.connect(config.getMailHost(), config.getMailUser(), null);
            return transport;
        } catch (Exception e) {
            throw Incidents.named("scireum.biz.InvalidMailConfiguration")
                           .withException(e)
                           .set("host", config.getMailHost())
                           .set("user", config.getMailUser())
                           .set("hasPassword", String.valueOf(!Tools.emptyString(config.getMailPassword())))
                           .handle();
        }
    }

    @Part
    private ArtifactManager am;

    private static class Attachment implements DataSource {

        private String contentType;
        private byte[] buffer;
        private String name;

        public Attachment(String name, String mimeType, byte[] byteArray) {
            this.name = name;
            contentType = mimeType;
            buffer = byteArray;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(buffer);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }
    }

    @Override
    public MailSender createEmail() {
        return new MailSenderImpl();
    }

}
