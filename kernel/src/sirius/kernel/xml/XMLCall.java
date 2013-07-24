package sirius.kernel.xml;

import java.io.IOException;
import java.net.URL;

/**
 * Simple call to send XML to a server and receive XML back.
 */
public class XMLCall {

    private OutcallImpl outcall;

    public static XMLCall to(URL url) throws IOException {
        XMLCall result = new XMLCall();
        result.outcall = new OutcallImpl(url);
        result.outcall.setRequestProperty("Content-Type", "text/xml");
        return result;
    }

    public XMLStructuredOutput getOutput() throws IOException {
        return new XMLStructuredOutput(outcall.getOutput());
    }

    public XMLStructuredInput getInput() throws IOException {
        return new XMLStructuredInput(outcall.getInput(), true);
    }
}
