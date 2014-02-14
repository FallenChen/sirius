package sirius.web.templates;

import org.apache.velocity.app.Velocity;
import org.xhtmlrenderer.pdf.ITextRenderer;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;

import java.io.OutputStream;
import java.io.StringWriter;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.02.14
 * Time: 13:27
 * To change this template use File | Settings | File Templates.
 */
@Register(name = VelocityPDFContentHandler.PDF_VM)
public class VelocityPDFContentHandler implements ContentHandler {

    public static final String PDF_VM = "pdf-vm";

    @Override
    public boolean generate(Content.Generator generator, OutputStream out) throws Exception {
        if (!PDF_VM.equals(generator.getHandlerType()) && !generator.isTemplateEndsWith(".pdf.vm")) {
            return false;
        }

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

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
