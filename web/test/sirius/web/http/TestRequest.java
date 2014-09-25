/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import sirius.kernel.async.Barrier;
import sirius.kernel.async.CallContext;
import sirius.kernel.async.Promise;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provides a test or mock instance of {@link sirius.web.http.WebContext}.
 * <p>
 * Can be used to simulate a request sent to the web server. Additionally the {@link sirius.web.http.TestResponse}
 * returned by {@link #executeAndBlock()} or {@link #execute()} can be used to inspect what kind of response
 * was generated or which parameters were passed along.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/09
 */
public class TestRequest extends WebContext implements HttpRequest {

    private HttpHeaders testHeaders = new DefaultHttpHeaders();
    private String testUri;
    private HttpMethod testMethod;
    private List<Cookie> testCookies = Lists.newArrayList();
    protected Promise<TestResponse> testResponsePromise = new Promise<>();

    private TestRequest() {
        super();
        this.request = this;
    }

    /**
     * Creates a mock request simulating a GET on the given uri.
     *
     * @param uri the relative uri to call
     * @return an instance used to further specify the request to send
     */
    public static TestRequest GET(String uri) {
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.GET;
        result.testUri = uri;
        return result;
    }

    /**
     * Creates a mock request simulating a GET on the given uri and query string parameters
     *
     * @param uri         the relative uri to call
     * @param queryString the parameters to be applied to the query string of the request.
     *                    Commonly known as "GET parameters".
     * @return an instance used to further specify the request to send
     */
    public static TestRequest GET(String uri, sirius.kernel.commons.Context queryString) {
        if (!queryString.isEmpty()) {
            uri += "?" + queryString.entrySet()
                                    .stream()
                                    .map(e -> e.getKey() + "=" + Strings.urlEncode(NLS.toMachineString(e.getValue())))
                                    .collect(Collectors.joining("&"));
        }
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.GET;
        result.testUri = uri;
        return result;
    }

    /**
     * Creates a mock request simulating a PUT on the given uri while sending the given resource.
     * <p>
     * The resource will be resolved using {@link sirius.web.templates.Content}
     * </p>
     *
     * @param uri      the relative uri to call
     * @param resource the name of the resource to send
     * @return an instance used to further specify the request to send
     */
    public static TestRequest PUT(String uri, String resource) {
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.PUT;
        result.testUri = uri;
        return result;
    }

    /**
     * Creates a mock request simulating a PUT on the given uri while sending the given data.
     *
     * @param uri      the relative uri to call
     * @param resource the data to send to the server
     * @return an instance used to further specify the request to send
     */
    public static TestRequest PUT(String uri, InputStream resource) {
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.PUT;
        result.testUri = uri;
        return result;
    }

    /**
     * Creates a mock request simulating a POST on the given uri while sending the given resource.
     * <p>
     * The resource will be resolved using {@link sirius.web.templates.Content}
     * </p>
     *
     * @param uri      the relative uri to call
     * @param resource the name of the resource to send
     * @return an instance used to further specify the request to send
     */
    public static TestRequest POST(String uri, String resource) {
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.POST;
        result.testUri = uri;
        return result;
    }

    /**
     * Creates a mock request simulating a POST on the given uri while sending the given data.
     *
     * @param uri      the relative uri to call
     * @param resource the data to send to the server
     * @return an instance used to further specify the request to send
     */
    public static TestRequest POST(String uri, InputStream resource) {
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.POST;
        result.testUri = uri;
        return result;
    }

    /**
     * Creates a mock request simulating a POST on the given uri and form data
     *
     * @param uri      the relative uri to call
     * @param formData the parameters be included in the post request
     * @return an instance used to further specify the request to send
     */
    public static TestRequest POST(String uri, sirius.kernel.commons.Context formData) {
        TestRequest result = new TestRequest();
        result.testMethod = HttpMethod.POST;
        result.testUri = uri;
        return result;
    }

    /**
     * Adds a header included in the request.
     *
     * @param name  the name of the header to add.
     * @param value the value of the header to add
     * @return the request itself for fluent method calls
     */
    public TestRequest addHeader(HttpHeaders.Names name, Object value) {
        testHeaders.add(name.toString(), value);
        return this;
    }

    /**
     * Adds a cookie included in the request.
     *
     * @param name  the name of the cookie to add.
     * @param value the value of the cookie to add
     * @return the request itself for fluent method calls
     */
    public TestRequest addCookie(String name, String value) {
        boolean updated = false;
        for (Cookie cookie : testCookies) {
            if (Strings.areEqual(name, cookie.getName())) {
                cookie.setValue(value);
                updated = true;
            }
        }
        if (!updated) {
            testCookies.add(new DefaultCookie(name, value));
        }
        testHeaders.set(HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(testCookies));
        return this;
    }

    /**
     * Executes the request (dispatches it).
     * <p>
     * Returns a promise which will be full filled, once the called code produces a response. Use
     * {@link #executeAndBlock()} to wait until a response is generated.
     * </p>
     *
     * @return a promise which will contain the response generated by the application
     */
    public Promise<TestResponse> execute() {
        CallContext.getCurrent().set(WebContext.class, this);
        for (WebDispatcher dispatcher : WebServerInitializer.getSortedDispatchers()) {
            try {
                if (dispatcher.dispatch(this)) {
                    return testResponsePromise;
                }
            } catch (Exception e) {
                throw Exceptions.handle(e);
            }
        }
        throw Exceptions.handle().withSystemErrorMessage("No Dispatcher found for request: " + getUri()).handle();
    }

    /**
     * Executes (dispatches) the request and waits until a response is generated.
     *
     * @return the generated response
     */
    public TestResponse executeAndBlock() {
        Barrier barrier = Barrier.create();
        barrier.add(execute());
        if (barrier.await(60, TimeUnit.SECONDS)) {
            if (testResponsePromise.isSuccessful()) {
                return testResponsePromise.get();
            } else {
                throw Exceptions.handle()
                                .withSystemErrorMessage("Failed to create a response for: %s", getUri())
                                .error(testResponsePromise.getFailure())
                                .handle();
            }
        } else {
            throw Exceptions.handle()
                            .withSystemErrorMessage("No response was created after 60s: %s", getUri())
                            .handle();
        }
    }

    @Override
    public Response respondWith() {
        return new TestResponse(this);
    }

    @Override
    public ChannelHandlerContext getCtx() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    protected void setCtx(ChannelHandlerContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpMethod getMethod() {
        return testMethod;
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUri() {
        return testUri;
    }

    @Override
    public HttpRequest setUri(String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return HttpVersion.HTTP_1_1;
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders headers() {
        return testHeaders;
    }

    @Override
    public DecoderResult getDecoderResult() {
        return DecoderResult.SUCCESS;
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "TestRequest: " + getUri();
    }
}
