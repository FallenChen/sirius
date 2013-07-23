/*******************************************************************************
 * Made with all the love in the world by scireum in Remshalden, Germany
 *
 * Copyright (c) 2013 scireum GmbH
 * http://www.scireum.de - info@scireum.de
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

package sirius.kernel;

import com.google.common.base.Objects;
import sirius.kernel.commons.Callback;
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
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13
 * Time: 11:15
 * To change this template use File | Settings | File Templates.
 */
public class Classpath {

    protected static final Log LOG = Log.get("classpath");
    private List<URL> componentRoots;
    private String componentName;

    public Classpath(String componentName) {
        this.componentName = componentName;
    }

    private List<String> getChildren(URL url) {
        List<String> result = new ArrayList<String>();
        if ("file".equals(url.getProtocol())) {
            File file = new File(url.getPath());
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            addFiles(file, result, file);
        } else if ("jar".equals(url.getProtocol())) {
            try {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> e = jar.entries();
                while (e.hasMoreElements()) {
                    JarEntry entry = e.nextElement();
                    result.add(entry.getName());
                }
            } catch (IOException e) {
                LOG.WARN(e);
            }
        }
        return result;
    }

    private void addFiles(File file, List<String> result, File reference) {
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                addFiles(child, result, reference);
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
                result.add(path);
            }
        }
    }

    public void find(Pattern pattern, Callback<Matcher> collector) {
        for (URL url : getComponentRoots()) {
            for (String relativePath : getChildren(url)) {
                Matcher matcher = pattern.matcher(relativePath);
                if (matcher.matches()) {
                    try {
                        collector.invoke(matcher);
                    } catch (Throwable e) {
                        LOG.SEVERE(e);
                    }
                }
            }
        }
    }

    public List<URL> getComponentRoots() {
        if (componentRoots == null) {
            try {
                componentRoots = Collections.list(getClass().getClassLoader().getResources(componentName));
            } catch (IOException e) {
                LOG.SEVERE(e);
            }
        }
        return componentRoots;
    }
}
