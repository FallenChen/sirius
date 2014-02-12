package sirius.web.templates;

import org.apache.velocity.app.Velocity;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.02.14
 * Time: 13:27
 * To change this template use File | Settings | File Templates.
 */
@Register(name = VelocityContentHandler.VM)
public class VelocityContentHandler implements ContentHandler {

    public static final String VM = "vm";

    @Override
    public boolean generate(Content.Generator generator, OutputStream out) throws Exception {
        if (!VM.equals(generator.getHandlerType()) && !Strings.isFilled(generator.getVelocityString()) && !generator.isTemplateEndsWith(
                ".vm")) {
            return false;
        }

        ScriptingContext ctx = new ScriptingContext();
        generator.getContext().applyTo(ctx);

        OutputStreamWriter writer = new OutputStreamWriter(out);
        if (Strings.isFilled(generator.getVelocityString())) {
            Velocity.evaluate(ctx, writer, "velocity", generator.getVelocityString());
        } else {
            Velocity.mergeTemplate(generator.getTemplateName(), generator.getEncoding(), ctx, writer);
        }
        writer.close();

        return true;
    }
}
