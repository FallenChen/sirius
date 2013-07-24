/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel;

import com.google.common.base.Objects;
import sirius.kernel.commons.BasicCollector;
import sirius.kernel.commons.Collector;
import sirius.kernel.health.Log;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieves a filtered list of resources in the classpath.
 * <p>
 * This is used by the {@link sirius.kernel.di.Injector} to discover and register all classes in the
 * component model. Additionally {@link sirius.kernel.nls.Babelfish} uses this to load all relevant .properties files.
 * </p>
 * <p>
 * The method used, is to provide a name of a resource which is placed in every component root (jar file etc.) which
 * then can be discovered using <tt>Class.getResources</tt>.
 * </p>
 * <p>
 * Once a file pattern is given, all files in the classpath are scanned, starting from the detected roots.
 * </p>
 *
 * @author Andreas Haufler (aha@sciruem.de)
 * @since 1.0
 */
public class Classpath {

    /**
     * Logger used to log problems when scanning the classpath
     */
    protected static final Log LOG = Log.get("classpath");
    private List<URL> componentRoots;
    private ClassLoader loader;
    private String componentName;

    /**
     * Creates a new Classpath, scanning for component roots with the given name
     *
     * @param loader        the class loader used to load the components
     * @param componentName the file name to identify component roots
     */
    public Classpath(ClassLoader loader, String componentName) {
        this.loader = loader;
        this.componentName = componentName;
    }

    /**
     * Returns the class loader used to load the classpath
     *
     * @return the class loader used by the framework
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Returns all detected component roots
     *
     * @return a list of URLs pointing to the component roots
     */
    public List<URL> getComponentRoots() {
        if (componentRoots == null) {
            try {
                componentRoots = Collections.list(loader.getResources(componentName));
            } catch (IOException e) {
                LOG.SEVERE(e);
            }
        }
        return componentRoots;
    }

    /**
     * Scans the classpath for files which relative path match the given patter
     *
     * @param pattern   the pattern for the relative path used to filter files
     * @param collector will be provided with all files in the classpath matching the given pattern
     */
    public void find(final Pattern pattern, final Collector<Matcher> collector) {
        for (URL url : getComponentRoots()) {
            scan(url, new BasicCollector<String>() {

                @Override
                public void add(String relativePath) {
                    Matcher matcher = pattern.matcher(relativePath);
                    if (matcher.matches()) {
                        try {
                            collector.add(matcher);
                        } catch (Throwable e) {
                            LOG.SEVERE(e);
                        }
                    }
                }
            });
        }
    }

    /*
     * Scans all files below the given root URL supplying files to the given collector. This
     * can handle file:// and jar:// URLs
     */
    private void scan(URL url, Collector<String> collector) {
        List<String> result = new ArrayList<String>();
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.getPath());
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            addFiles(file, collector, file);
        } else if ("jar".equals(url.getProtocol())) {
            try {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> e = jar.entries();
                while (e.hasMoreElements()) {
                    JarEntry entry = e.nextElement();
                    collector.add(entry.getName());
                    result.add(entry.getName());
                }
            } catch (IOException e) {
                LOG.SEVERE(e);
            }
        }
    }

    /*
     * DFS searcher for file / directory based classpath elements
     */
    private void addFiles(File file, Collector<String> collector, File reference) {
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                addFiles(child, collector, reference);
            } else {
                String path = null;
                while (child != null && !Objects.equal(child, reference)) {
                    if (path != null) {
                        path = child.getName() + "/" + path;
                    } else {
                        path = child.getName();
                    }
                    child = child.getParentFile();
                }
                collector.add(path);
            }
        }
    }
}
