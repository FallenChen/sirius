package sirius.web.mails;

import com.scireum.common.MultipartMailReader;
import com.scireum.common.Tools;
import com.scireum.common.Tuple;
import com.scireum.common.data.Context;
import com.scireum.ocm.annotations.Part;
import com.scireum.ocm.annotations.Parts;
import com.scireum.ocm.annotations.Register;
import com.scireum.ocm.config.ConfigValue;
import com.scireum.ocm.db.OMA;
import com.scireum.ocm.db.security.Realm;
import com.scireum.ocm.health.EmailLogEntry;import com.scireum.ocm.health.EmailLogEntry.MailStatus;
import com.scireum.ocm.health.Syslog;import com.scireum.ocm.incidents.Incidents;
import com.scireum.ocm.mails.MailService;
import com.scireum.ocm.mails.MailServiceBean;
import com.scireum.ocm.timer.EveryHour;
import com.scireum.ocm.web.servlet.content.ContentGenerator;

import javax.mail.BodyPart;import javax.mail.Flags;import javax.mail.Folder;import javax.mail.Message;import javax.mail.MessagingException;import javax.mail.NoSuchProviderException;import javax.mail.Session;import javax.mail.Store;import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.lang.Exception;import java.lang.Override;import java.lang.String;import java.lang.Throwable;import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes care of bounced emails.
 */
@Register(classes = EveryHour.class)
public class BounceProcessor implements EveryHour {

    private static final String BOUNCE_TOKEN = "BOUNCE";

    public static final Pattern MESSAGE_ID = Pattern.compile("Message\\-Id: *(.*)\\s?", Pattern.CASE_INSENSITIVE);

    @Part
    private MailService ms;

    @Parts(BounceHandler.class)
    private List<BounceHandler> bounceHandlers;

    @Override
    public void runTimer() throws Exception {
        Store store = null;
        Folder folder = null;
        int numFetched = 0;
        int numBounced = 0;
        try {
            store = connectPop3();
            if (store == null) {
                return;
            }
            folder = store.getDefaultFolder();
            if (folder == null) {
                throw new Exception("No default folder");
            }
            folder = folder.getFolder("INBOX");
            if (folder == null) {
                throw new Exception("No POP3 INBOX");
            }
            folder.open(Folder.READ_WRITE);
            for (Message msg : folder.getMessages()) {
                if (folder.isOpen()) {
                    if (handleMessage(msg)) {
                        numBounced++;
                    }
                }
                numFetched++;
            }

        } catch (Throwable ex) {
            Incidents.handle(ex);
        } finally {
            try {
                if (folder != null) {
                    folder.close(true);
                }
                if (store != null) {
                    store.close();
                }
            } catch (Exception ex2) {
                Incidents.handle(ex2);
            }
        }
        if (numFetched > 0) {
            Syslog.log("MAIL-BOUNCE", "Scanned: " + numFetched + " messages and found: " + numBounced + " bounces.");
        }
    }

    protected Store connectPop3() throws NoSuchProviderException, MessagingException {
        Store store;
        String popServer = ConfigValue.getStringValue("Pop3BounceHandlerServer", "");
        String popUser = ConfigValue.getStringValue("Pop3BounceHandlerUser", "");
        String popPassword = ConfigValue.getStringValue("Pop3BounceHandlerPassword", "");
        if (Tools.emptyString(popServer)) {
            return null;
        }
        Properties props = java.lang.System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        store = session.getStore("pop3");
        store.connect(popServer, popUser, popPassword);
        return store;
    }

    protected boolean handleMessage(Message msg) throws MessagingException, IOException, Exception {
        MimeMessage originalMessage = null;

        if (msg.isMimeType("multipart/report") && msg.getContent() instanceof MimeMultipart) {
            originalMessage = getOriginalMessage((MimeMultipart) msg.getContent());
        }
        Tuple<String, String> content = MultipartMailReader.readContent(msg);
        String stringContents = content.getFirst();
        if (Tools.emptyString(stringContents)) {
            stringContents = content.getSecond();
        }
        if (Tools.emptyString(stringContents)) {
            return false;
        }
        String messageId = null;
        if (originalMessage != null) {
            messageId = originalMessage.getMessageID();
        }
        if (Tools.emptyString(messageId)) {
            Matcher m = MESSAGE_ID.matcher(stringContents);
            if (m.find()) {
                messageId = m.group(1);
            }
        }
        if (Tools.notEmpty(messageId)) {
            handleBounce(msg, originalMessage, stringContents, messageId);
            return true;
        }
        return false;
    }

    protected void handleBounce(Message msg,
                                MimeMessage originalMessage,
                                String stringContents,
                                String messageId) throws MessagingException {
        EmailLogEntry entry = OMA.select(Realm.SYSTEM, EmailLogEntry.class)
                                 .eq(messageId, EmailLogEntry.MESSAGE_ID)
                                 .first();
        if (entry != null) {
            Syslog.log("MAIL-BOUNCE",
                       "Bounce from: " + ((InternetAddress) msg.getFrom()[0]).getAddress() + " - " + entry.getSubject());
            entry.setBounceContent(stringContents);
            entry.setStatus(MailStatus.BOUNCED);
            OMA.saveEntity(Realm.SYSTEM, entry);
            msg.setFlag(Flags.Flag.DELETED, true);
            if (originalMessage != null) {
                String[] bounceTokens = originalMessage.getHeader(MailServiceBean.X_BOUNCETOKEN);
                String bounceToken = null;
                if (bounceTokens != null && bounceTokens.length > 0) {
                    bounceToken = bounceTokens[0];
                }
                processBounceToken(bounceToken, originalMessage);
                if (ConfigValue.get("Pop3BounceHandlerDoForward", "false").asBoolean(false)) {
                    forwardMessage(originalMessage, stringContents, bounceToken);
                }
            }
        }
    }

    protected void forwardMessage(MimeMessage originalMessage,
                                  String stringContents,
                                  String bounceToken) throws MessagingException {
        if (originalMessage.getFrom().length > 0 && !Tools.equal(originalMessage.getSender(),
                                                                 originalMessage.getFrom()[0]) && !BOUNCE_TOKEN.equals(
                bounceToken)) {
            Context ctx = ContentGenerator.createInitializedContext();
            ctx.set("originalSubject", originalMessage.getSubject());
            ctx.set("originalFrom", originalMessage.getFrom()[0].toString());
            ctx.set("originalTo", originalMessage.getRecipients(Message.RecipientType.TO)[0].toString());
            ctx.set("originalMsg", MultipartMailReader.readContent(originalMessage).getFirst());
            ctx.set("bounceMsg", stringContents);
            String sender = ((InternetAddress) originalMessage.getFrom()[0]).getAddress();
            Syslog.log("MAIL-BOUNCE-FWD", "Forwarded message to " + sender + ": " + originalMessage.getSubject());
            ms.createEmail()
              .useMailExtension("scireum.health.bounce-mail")
              .setBounceToken(BOUNCE_TOKEN)
              .applyContext(ctx)
              .toEmail(sender)
              .toName(((InternetAddress) originalMessage.getFrom()[0]).getPersonal())
              .send();
        }
    }


    protected void processBounceToken(String bounceToken, MimeMessage originalMessage) throws MessagingException {
        if (Tools.emptyString(bounceToken)) {
            return;
        }
        for (BounceHandler handler : bounceHandlers) {
            try {
                handler.handleBounce(bounceToken, originalMessage);
            } catch (Throwable e) {
                Incidents.handle(e);
            }
        }
    }

    private MimeMessage getOriginalMessage(MimeMultipart mm) throws Exception {
        for (int i = 0; i < mm.getCount(); i++) {
            BodyPart mbp = mm.getBodyPart(i);
            if (mbp.getContent() instanceof MimeMessage) {
                return (MimeMessage) mbp.getContent();
            }
        }
        return null;
    }

}
