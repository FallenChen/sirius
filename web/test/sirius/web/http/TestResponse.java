/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.async.Promise;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.health.Exceptions;
import sirius.kernel.xml.StructuredNode;
import sirius.kernel.xml.XMLStructuredInput;

import javax.annotation.Nullable;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by aha on 23.09.14.
 */
public class TestResponse extends Response {
    protected TestResponse(TestRequest testRequest) {
        super(testRequest);
        responsePromise = testRequest.testResponsePromise;
    }

    public static enum ResponseType {
        STATUS, TEMPORARY_REDIRECT, PERMANENT_REDIRECT, FILE, RESOURCE, ERROR, DIRECT, TEMPLATE, TUNNEL, STREAM;
    }

    private ResponseType type;
    private HttpResponseStatus status;
    private String templateName;
    private List<Object> templateParameters = Lists.newArrayList();
    private String redirectUrl;
    private File file;
    private byte[] content;
    private String errorMessage;
    private String tunnelTargetUrl;
    private Promise<TestResponse> responsePromise;
    private JSONObject jsonContent;
    private XMLStructuredInput xmlContent;

    public HttpResponseStatus getStatus() {
        return status;
    }

    public ResponseType getType() {
        return type;
    }

    public String getTemplateName() {
        return templateName;
    }

    public List<Object> getTemplateParameters() {
        return templateParameters;
    }

    public Value getTemplateParameter(int index) {
        return Value.indexOf(index, templateParameters);
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public File getFile() {
        return file;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTunnelTargetUrl() {
        return tunnelTargetUrl;
    }

    public byte[] getRawContent() {
        return content;
    }

    public String getContentAsString() {
        return new String(content, Charsets.UTF_8);
    }

    public JSONObject getContentAsJson() {
        if (jsonContent == null) {
            jsonContent = JSON.parseObject(getContentAsString());
        }
        return jsonContent;
    }

    public XMLStructuredInput getContentAsXML() {
        if (xmlContent == null) {
            try {
                xmlContent = new XMLStructuredInput(new ByteArrayInputStream(content), true);
            } catch (IOException e) {
                throw Exceptions.handle(e);
            }
        }
        return xmlContent;
    }

    public StructuredNode xmlContent() {
        try {
            return getContentAsXML().getNode(".");
        } catch (XPathExpressionException e) {
            throw Exceptions.handle(e);
        }
    }


    @Override
    public void status(HttpResponseStatus status) {
        type = ResponseType.STATUS;
        this.status = status;
        responsePromise.success(this);
    }

    @Override
    public void redirectTemporarily(String url) {
        type = ResponseType.TEMPORARY_REDIRECT;
        status = HttpResponseStatus.TEMPORARY_REDIRECT;
        redirectUrl = url;
        responsePromise.success(this);
    }

    @Override
    public void redirectPermanently(String url) {
        type = ResponseType.PERMANENT_REDIRECT;
        status = HttpResponseStatus.MOVED_PERMANENTLY;
        redirectUrl = url;
        responsePromise.success(this);
    }

    @Override
    public void file(File file) {
        type = ResponseType.FILE;
        status = HttpResponseStatus.OK;
        this.file = file;
        responsePromise.success(this);
    }

    @Override
    public void resource(URLConnection urlConnection) {
        try {
            type = ResponseType.RESOURCE;
            status = HttpResponseStatus.OK;
            content = ByteStreams.toByteArray(urlConnection.getInputStream());
            responsePromise.success(this);
        } catch (IOException e) {
            responsePromise.fail(e);
        }
    }

    @Override
    public void error(HttpResponseStatus status, String message) {
        type = ResponseType.ERROR;
        this.status = status;
        this.errorMessage = message;
    }

    @Override
    public void direct(HttpResponseStatus status, String content) {
        type = ResponseType.DIRECT;
        this.status = status;
        this.content = content.getBytes(Charsets.UTF_8);
        responsePromise.success(this);
    }

    @Override
    public void template(String name, Object... params) {
        try {
            type = ResponseType.TEMPLATE;
            status = HttpResponseStatus.OK;
            templateName = name;
            templateParameters = Arrays.asList(params);
            super.template(name, params);
        } catch (Throwable e) {
            responsePromise.fail(e);
        }
    }

    @Override
    public void nlsTemplate(String name, Object... params) {
        try {
            type = ResponseType.TEMPLATE;
            status = HttpResponseStatus.OK;
            templateName = name;
            templateParameters = Arrays.asList(params);
            super.nlsTemplate(name, params);
        } catch (Throwable e) {
            responsePromise.fail(e);
        }
    }

    @Override
    protected void sendTemplateContent(String name, String content) {
        this.content = content.getBytes(Charsets.UTF_8);
        responsePromise.success(this);
    }

    @Override
    public void tunnel(String url) {
        type = ResponseType.TUNNEL;
        status = HttpResponseStatus.OK;
        tunnelTargetUrl = url;
        responsePromise.success(this);
    }

    @Override
    public OutputStream outputStream(HttpResponseStatus status, @Nullable String contentType) {
        type = ResponseType.STREAM;
        this.status = status;
        if (Strings.isFilled(contentType)) {
            addHeaderIfNotExists(HttpHeaders.Names.CONTENT_TYPE, contentType);
        }
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                content = toByteArray();
                responsePromise.success(TestResponse.this);
            }
        };
    }
}
