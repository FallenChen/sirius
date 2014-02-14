package sirius.web.templates;

import com.google.common.base.Charsets;
import org.apache.velocity.app.Velocity;
import org.xhtmlrenderer.pdf.ITextRenderer;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.xml.XMLStructuredOutput;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.02.14
 * Time: 13:27
 * To change this template use File | Settings | File Templates.
 */
@Register(name = JsXMLContentHandler.XML_JS)
public class JsXMLContentHandler implements ContentHandler {

    public static final String XML_JS = "xml-js";

    @ConfigValue("content.script-engine")
    private String scriptEngine;

    @Override
    public boolean generate(Content.Generator generator, OutputStream out) throws Exception {
        if (!XML_JS.equals(generator.getHandlerType()) && !generator.isTemplateEndsWith(".xml.js")) {
            return false;
        }

        XMLStructuredOutput xmlOut = new XMLStructuredOutput(out);
        generator.getContext().put("xml", xmlOut);
        xmlOut.beginResult();
        execute(generator);
        xmlOut.endResult();

        ScriptingContext ctx = new ScriptingContext();
        generator.getContext().applyTo(ctx);

        StringWriter writer = new StringWriter();
        if (Strings.isFilled(generator.getVelocityString())) {
            Velocity.evaluate(ctx, writer, "velocity", generator.getVelocityString());
        } else {
            Velocity.mergeTemplate(generator.getTemplateName(), generator.getEncoding(), ctx, writer);
        }

        ITextRenderer renderer = new ITextRenderer();
        renderer.getSharedContext()
                .setReplacedElementFactory(new BarcodeReplacedElementFactory(renderer.getOutputDevice()));
        renderer.setDocumentFromString(writer.toString());
        renderer.layout();
        renderer.createPDF(out);
        out.flush();
        writer.close();

        return true;
    }

    private final ScriptEngineManager manager = new ScriptEngineManager();

    private void execute(Content.Generator generator) throws Exception {
        ScriptEngine engine = manager.getEngineByName(scriptEngine);
        ScriptingContext ctx = new ScriptingContext();
        generator.getContext().applyTo(ctx);
        if (Strings.isFilled(generator.getVelocityString())) {
            engine.eval(generator.getVelocityString(), ctx);
        } else {
            engine.put(ScriptEngine.FILENAME, generator.getTemplateName());
            Reader reader = new InputStreamReader(generator.getTemplate(), Charsets.UTF_8);
            try {
                engine.eval(reader, ctx);
            } finally {
                reader.close();
            }
        }
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
