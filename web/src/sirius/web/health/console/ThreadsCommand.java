package sirius.web.health.console;

import sirius.kernel.di.annotations.Register;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 28.07.13
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
@Register(name = "threads")
public class ThreadsCommand implements Command {

    private ThreadMXBean t = ManagementFactory.getThreadMXBean();

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length == 1) {
            for (Map.Entry<Thread, StackTraceElement[]> thread : Thread.getAllStackTraces().entrySet()) {
                if ("all".equalsIgnoreCase(params[0]) || thread.getKey()
                                                               .getName()
                                                               .toLowerCase()
                                                               .contains(params[0].toLowerCase())) {
                    output.blankLine();
                    output.line(thread.getKey().getName());
                    output.separator();
                    for (StackTraceElement e : thread.getValue()) {
                        output.apply("%-60s %19s",e.getClassName() + "." + e.getMethodName(), e.getFileName() + ":" + e.getLineNumber());
                    }
                    output.separator();
                    output.blankLine();
                }
            }
        } else {
            output.apply("%-15s %10s %53s", "STATE", "ID","NAME");
            output.separator();
            for(ThreadInfo info : t.dumpAllThreads(false, false)) {
                output.apply("%-15s %10s %53s", info.getThreadState().name(), info.getThreadId(), info.getThreadName());
            }
            output.separator();
        }
    }

    @Override
    public String getName() {
        return "threads";
    }

    @Override
    public String getDescription() {
        return "Reports a list of all threads or creates a stack trace of a given tread or all threads (using 'all' as param)";
    }
}
