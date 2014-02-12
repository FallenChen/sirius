package sirius.web.templates;

import sirius.kernel.Sirius;
import sirius.kernel.commons.Context;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Register;
import sirius.kernel.nls.NLS;

import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.02.14
 * Time: 13:47
 * To change this template use File | Settings | File Templates.
 */
@Register
public class DefaultContentContextExtender implements ContentContextExtender {

    @sirius.kernel.di.std.Context
    private GlobalContext ctx;

    @Override
    public void extend(Context context) {
        context.put("ctx", ctx);
        context.put("config", Sirius.getConfig());
        context.put("product", Sirius.getProductName());
        context.put("version", Sirius.getProductVersion());
        context.put("nls", NLS.class);
        context.put("log", Content.LOG);

        Calendar now = Calendar.getInstance();
        context.put("now", now);
        context.put("date", NLS.toUserString(now, false));
        context.put("time", NLS.getTimeFormat(NLS.getCurrentLang()).format(now.getTime()));
        context.put("year", now.get(Calendar.YEAR));
        context.put("month", now.get(Calendar.MONTH) + 1);
        context.put("day", now.get(Calendar.DAY_OF_MONTH));
        context.put("hour", now.get(Calendar.HOUR_OF_DAY));
        context.put("minute", now.get(Calendar.MINUTE));
        context.put("second", now.get(Calendar.SECOND));
    }
}
