package sirius.web.templates;

import sirius.kernel.di.std.Priorized;

import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.02.14
 * Time: 12:57
 * To change this template use File | Settings | File Templates.
 */
public interface ContentHandler extends Priorized {

    boolean generate(Content.Generator generator, OutputStream out) throws Exception;

}
