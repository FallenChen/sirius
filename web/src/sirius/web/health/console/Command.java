package sirius.web.health.console;

import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 28.07.13
 * Time: 19:35
 * To change this template use File | Settings | File Templates.
 */
public interface Command {

    interface Output {
        PrintWriter getWriter();

        Output blankLine();

        Output line(String contents);

        Output separator();

        Output apply(String format, Object... columns);
    }

    /**
     * Executes the given command with the given parameters.
     */
    void execute(Output output, String... params) throws Exception;

    /**
     * Returns the name of the command.
     */
    String getName();

    /**
     * Returns a short description of the command.
     */
    String getDescription();
}
