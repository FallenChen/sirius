/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.Parts;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 15:08
 * To change this template use File | Settings | File Templates.
 */
public class WebServerPipelineFactory implements ChannelPipelineFactory {

    @Parts(WebDispatcher.class)
    private PartCollection<WebDispatcher> dispatchers;
    private List<WebDispatcher> sortedDispatchers;

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("deflater", new HttpContentCompressor());
        pipeline.addLast("handler", new WebServerHandler(getSortedDispatchers()));

        return pipeline;
    }

    private List<WebDispatcher> getSortedDispatchers() {
        if (sortedDispatchers == null) {
            PriorityCollector<WebDispatcher> collector = PriorityCollector.create();
            for (WebDispatcher wd : dispatchers.getParts()) {
                collector.add(wd.getPriority(), wd);
            }
            sortedDispatchers = collector.getData();
        }
        return sortedDispatchers;
    }
}
