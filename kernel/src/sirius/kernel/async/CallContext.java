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

package sirius.kernel.async;

import org.apache.log4j.MDC;
import sirius.kernel.Sirius;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Watch;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 10:25
 * To change this template use File | Settings | File Templates.
 */
public class CallContext {

    private static ThreadLocal<CallContext> currentContext = new ThreadLocal<CallContext>();
    private static String nodeName;

    public static CallContext getCurrent() {
        CallContext result = currentContext.get();
        if (result == null) {
            return initialize();
        }

        return result;
    }

    public static void installInThread(CallContext ctx) {
        currentContext.set(ctx);
    }

    private static CallContext initialize(String externalFlowId) {
        CallContext ctx = new CallContext();
        ctx.addToMDC("flow", externalFlowId);
        ctx.setLang(NLS.getDefaultLanguage());
        currentContext.set(ctx);
        return ctx;
    }

    public static CallContext initialize() {
        return initialize(UUID.randomUUID().toString());
    }

    public static void setCurrent(CallContext context) {
        currentContext.set(context);
    }

    private Map<String, String> mdc = new LinkedHashMap<String, String>();
    private Map<Class<?>, Object> subContext = new HashMap<Class<?>, Object>();
    private Watch watch = Watch.start();

    private String lang = NLS.getDefaultLanguage();

    public String getNodeName() {
        if (nodeName == null) {
            if (Sirius.getConfig().hasPath("sirius.nodeName")) {
                nodeName = Sirius.getConfig().getString("sirius.nodeName");
            } else {
                try {
                    nodeName = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    Exceptions.handle(e);
                    nodeName = "unknown";
                }
            }
        }

        return nodeName;
    }

    public List<Tuple<String, String>> getMDC() {
        return Tuple.fromMap(mdc);
    }

    public Watch getWatch() {
        return watch;
    }

    public void addToMDC(String key, String value) {
        mdc.put(key, value);
    }

    public void removeFromMDC(String key) {
        mdc.remove(key);
    }

    public <C> C get(Class<C> contextType) {
        try {
            Object result = subContext.get(contextType);
            if (result == null) {
                result = contextType.newInstance();
                subContext.put(contextType, result);
            }

            return (C) result;
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .error(e)
                            .withSystemErrorMessage("Cannot get instance of %s from current CallContext: %s (%s)",
                                                    contextType.getName())
                            .handle();
        }
    }

    public void applyToLog4j() {
        Hashtable<String, String> ctx = MDC.getContext();
        if (ctx == null) {
            for (Map.Entry<String, String> e : mdc.entrySet()) {
                MDC.put(e.getKey(), e.getValue());
            }
        } else {
            ctx.clear();
            ctx.putAll(mdc);
        }
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

}
