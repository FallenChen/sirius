/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

/**
 * Participates in the dispatching process for incoming HTTP requests.
 * <p>
 * Once a HTTP request is fully read by the server it is dispatched calling all available <tt>WebDispatcher</tt>
 * instances available. To provide a WebDispatch, a subclass therefore needs to wear an
 * {@link sirius.kernel.di.std.Register} annotation.
 * </p>
 * <p>
 * As the first request arrives, all dispatchers are asked for their priority (via {@link #getPriority()} and
 * then sorted according to that in an ascending manner. As default priority
 * {@link sirius.kernel.commons.PriorityCollector#DEFAULT_PRIORITY} can be used. Note that this will only be done
 * once, therefore <tt>getPriority()</tt> must only return a constant value as it is never re-evaluated. By default,
 * a not found handler ({@link sirius.web.dispatch.NotFoundDispatcher} is registered with 999 as priority, so higher
 * priorities will never be executed.
 * </p>
 * <p>
 * For each incoming request, the list of dispatchers is iterated and {@link #dispatch(WebContext)} is invoked
 * until one of those returns <tt>true</tt>, to signal that the request was handled.
 * </p>
 * <p>
 * If a dispatcher performs serious work or any blocking IO operation. The dispatcher must complete the request
 * in another thread (using {@link sirius.kernel.async.Async#executor(String)}
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see WebServerHandler
 * @since 2013/08
 */
public interface WebDispatcher {

    /**
     * Returns the priority to determine the position in the dispatcher list.
     * <p>
     * Dispatchers are sorted ascending (lower is better). The default priority is
     * {@link sirius.kernel.commons.PriorityCollector#DEFAULT_PRIORITY}, the max. value is 998 as everything above
     * will be handled by the {@link sirius.web.dispatch.NotFoundDispatcher}.
     * </p>
     *
     * @return the priority of the dispatcher
     */
    int getPriority();

    /**
     * Invoked in order to handle the given request.
     * <p>
     * If the dispatcher doesn't feel responsible for handling the request, it simply returns <tt>false</tt>. Otherwise
     * if the request is being handled, <tt>true</tt> must be returned
     * </p>
     * <p>
     * Note that no blocking operation must be performed in this method. For any complex interaction, a new thread
     * should be forked using {@link sirius.kernel.async.Async#executor(String)}. Note that even
     * {@link Response#outputStream(org.jboss.netty.handler.codec.http.HttpResponseStatus, String, Integer)} might
     * block sooner or later to limit heap memory usage - to fork a thread for any serious work besides checking
     * responsibilities for handling requests.
     * </p>
     *
     * @param ctx the request to handle
     * @return <tt>true</tt> if the request was handled by this dispatcher, <tt>false</tt> otherwise.
     * @throws Exception in case of an error when parsing or dispatching the request
     */
    boolean dispatch(WebContext ctx) throws Exception;

}
