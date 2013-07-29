package sirius.web.health.console;

import com.google.common.hash.Hashing;
import sirius.kernel.di.annotations.Register;

@Register(name = "md5")
public class MD5Command implements Command {

    @Override
    public void execute(Output output, String... params) throws Exception {
        if (params.length < 1) {
            output.line("Usage: md5 <input>");
        } else {
            output.line("Input: " + params[0]);
            output.line("Timestamp: " + System.currentTimeMillis() / 1000);
            output.line("MD5: " + Hashing.md5().hashString(params[0]).toString());
        }
    }

    @Override
    public String getName() {
        return "md5";
    }

    @Override
    public String getDescription() {
        return "Computes the md5 hash of the given input";
    }

}
