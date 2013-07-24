package sirius.kernel;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 24.07.13
 * Time: 09:55
 * To change this template use File | Settings | File Templates.
 */
public class IPL {

    private static ClassLoader loader = ClassLoader.getSystemClassLoader();

    public static void main(String[] args) {
        boolean debug = Boolean.parseBoolean(System.getProperty("debug"));
        boolean ide = Boolean.parseBoolean(System.getProperty("ide"));
        File home = new File(System.getProperty("user.dir"));
        if (debug) {
            System.out.println("I N I T I A L   P R O G R A M   L O A D");
            System.out.println("---------------------------------------");
            System.out.println("IPL from: " + home.getAbsolutePath());
        }

        if (!ide) {
            try {
                File jars = new File(home, "lib");
                if (jars.exists()) {
                    loader = new URLClassLoader(allJars(jars), loader);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            try {
                File classes = new File(home, "classes");
                if (classes.exists()) {
                    loader = new URLClassLoader(new URL[]{classes.toURI().toURL()}, loader);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("IPL from IDE: not loading any classes or jars!");
        }

        try {
            if (debug) {
                System.out.println("IPL completed - Loading Sirius as stage2...");
            }
            Class.forName("sirius.kernel.Sirius", true, loader)
                 .getMethod("initializeEnvironment", ClassLoader.class)
                 .invoke(null, loader);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static URL[] allJars(File libs) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (File file : libs.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                urls.add(file.toURI().toURL());
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }
}
