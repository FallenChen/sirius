package sirius.kernel.xml;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import sirius.kernel.commons.Context;
import sirius.kernel.commons.Strings;
import sirius.kernel.nls.NLS;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class Outcall {

    private HttpURLConnection connection;
    private final URL url;

    public Outcall(URL url) throws IOException {
        this.url = url;
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
    }

    public Outcall(URL url, Context params) throws IOException {
        this.url = url;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStreamWriter writer = new OutputStreamWriter(getOutput(), Charsets.ISO_8859_1.name());
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), Charsets.ISO_8859_1.name()));
            sb.append("=");
            sb.append(URLEncoder.encode(NLS.toMachineString(entry.getValue()), Charsets.ISO_8859_1.name()));
        }
        writer.write(sb.toString());
        writer.flush();
    }

    public InputStream getInput() throws IOException {
        return connection.getInputStream();
    }

    public OutputStream getOutput() throws IOException {
        return connection.getOutputStream();
    }

    public void setRequestProperty(String name, String value) {
        connection.setRequestProperty(name, value);
    }

    public void setAuthParams(String user, String password) throws IOException {
        if (Strings.isEmpty(user)) {
            return;
        }
        try {
            String userpassword = user + ":" + password;
            String encodedAuthorization = BaseEncoding.base64()
                                                      .encode(userpassword.getBytes(Charsets.ISO_8859_1.name()));
            setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    public String getData() throws IOException {
        StringWriter writer = new StringWriter();
        InputStreamReader reader = new InputStreamReader(getInput(),
                                                         connection.getContentEncoding() == null ? Charsets.ISO_8859_1
                                                                                                           .name() : connection
                                                                 .getContentEncoding());
        CharStreams.copy(reader, writer);
        reader.close();
        return writer.toString();
    }

    public void setCookie(String name, String value) {
        if (Strings.isFilled(name) && Strings.isFilled(value)) {
            setRequestProperty("Cookie", name + "=" + value);
        }
    }

}
