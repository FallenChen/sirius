/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.templates;

import org.rythmengine.resource.ITemplateResource;
import org.rythmengine.resource.TemplateResourceBase;
import org.rythmengine.utils.IO;

import java.net.URL;

/**
 * Used as {@link org.rythmengine.resource.ITemplateResource} implementation created by our
 * {@link sirius.web.templates.Resolver} framework.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/05
 */
class URLTemplateResource extends TemplateResourceBase implements ITemplateResource {

    private String key;
    private URL url;

    public URLTemplateResource(String key, URL url) {
        this.key = key;
        this.url = url;
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
        if (obj instanceof URLTemplateResource) {
            URLTemplateResource that = (URLTemplateResource) obj;
            return that.url.equals(this.url);
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

    @Override
    public String toString() {
        return key + ": " + url;
    }
}
