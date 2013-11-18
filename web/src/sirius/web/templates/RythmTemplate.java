/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.templates;

import org.rythmengine.Rythm;
import org.rythmengine.RythmEngine;
import org.rythmengine.resource.ITemplateResource;
import org.rythmengine.resource.TemplateResourceBase;
import org.rythmengine.utils.IO;
import sirius.kernel.Sirius;

import java.net.URL;

/**
 * Implementation of {@link ITemplateResource} to get caching and reloading right.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
class RythmTemplate extends TemplateResourceBase implements ITemplateResource {

    private URL url;
    private String key;

    /**
     * Creates a new template for the given path within the given engine.
     *
     * @param path   the path to the template source
     * @param engine the surrounding engine
     */
    RythmTemplate(String path, RythmEngine engine) {
        super(engine);
        ClassLoader cl = Sirius.getClasspath().getLoader();
        if (null == cl) {
            cl = Rythm.class.getClassLoader();
        }
        // strip leading slash so path will work with classes in a JAR file
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        url = cl.getResource(path);
        key = path.replace("/", ".");
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String reload() {
        return IO.readContentAsString(url);
    }

    @Override
    protected long lastModified() {
        if (engine().isProdMode()) return 0;

        String fileName;
        if ("file".equals(url.getProtocol())) {
            fileName = url.getFile();
        } else if ("jar".equals(url.getProtocol())) {
            try {
                java.net.JarURLConnection jarUrl = (java.net.JarURLConnection) url.openConnection();
                fileName = jarUrl.getJarFile().getName();
            } catch (Exception e) {
                return System.currentTimeMillis() + 1;
            }
        } else {
            return System.currentTimeMillis() + 1;
        }

        java.io.File file = new java.io.File(fileName);
        return file.lastModified();
    }

    @Override
    public boolean isValid() {
        return null != url;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof RythmTemplate) {
            RythmTemplate that = (RythmTemplate) obj;
            return that.getKey().equals(this.getKey());
        }
        return false;
    }

    @Override
    protected long defCheckInterval() {
        return -1;
    }

    @Override
    protected Long userCheckInterval() {
        return Long.valueOf(1000 * 5);
    }

    @Override
    public String getSuggestedClassName() {
        return path2CN(key);
    }

}
