package sirius.web.mails;

import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
* Created with IntelliJ IDEA.
* User: aha
* Date: 12.02.14
* Time: 18:12
* To change this template use File | Settings | File Templates.
*/
class Attachment implements DataSource {

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
