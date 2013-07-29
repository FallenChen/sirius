package sirius.web.health.console;

import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.annotations.Context;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;
import sirius.kernel.xml.StructuredOutput;
import sirius.web.http.services.ServiceCall;
import sirius.web.http.services.StructuredService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 13:58
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "system-console")
public class ConsoleService implements StructuredService {

    @Context
    private GlobalContext ctx;

    @Override
    public void call(ServiceCall call, StructuredOutput out) throws Exception {
        out.beginResult();
        try {
            Watch w = Watch.start();
//            if (!Users.isSystemTenant() && !Model.isDebugEnvironment()) {
//                throw new BusinessException("The System Console is only available for the System Tenant");
//            }
            Map<String, Object> map = call.getContext().getJSONContent();
            String command = (String) map.get("method");
            List<Object> params = (List<Object>) map.get("params");
            String[] strParams = new String[params.size()];
            int i = 0;
            for (Object val : params) {
                strParams[i++] = NLS.toMachineString(val);
            }
            Command cmd = ctx.getPart(command, Command.class);
            if (cmd == null) {
                throw new Exception("Unknown command: " + command + ". Use 'help' to get a list.");
            }
            StringWriter buffer = new StringWriter();
            final PrintWriter pw = new PrintWriter(buffer);
            pw.println();
            cmd.execute(new Command.Output() {
                @Override
                public PrintWriter getWriter() {
                    return pw;
                }

                @Override
                public Command.Output blankLine() {
                    pw.println();
                    return this;
                }

                @Override
                public Command.Output line(String contents) {
                    pw.println(contents);
                    return this;
                }

                @Override
                public Command.Output separator() {
                    return line("--------------------------------------------------------------------------------");
                }

                @Override
                public Command.Output apply(String format, Object... columns) {
                    return line(Strings.apply(format, columns));
                }
            }, strParams);
            pw.println(w.duration());
            pw.println();
            out.property("result", buffer.toString());
        } catch (Throwable t) {
            Exception e = Exceptions.handle(t);
            out.beginObject("error");
            out.property("code", t.getClass().getName());
            out.property("message", e.getMessage());
            out.endObject();
        } finally {
            out.endResult();
        }
    }

}
