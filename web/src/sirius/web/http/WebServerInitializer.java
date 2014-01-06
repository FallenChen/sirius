package sirius.web.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.joda.time.Duration;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Parts;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 05.01.14
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
public class WebServerInitializer extends ChannelInitializer<SocketChannel> {


    @Parts(WebDispatcher.class)
    private PartCollection<WebDispatcher> dispatchers;

    @ConfigValue("http.idleTimeout")
    private Duration idleTimeout;

    private List<WebDispatcher> sortedDispatchers;

    protected WebServerInitializer() {
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addFirst("lowlevel", LowLevelHandler.INSTANCE);
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        if (idleTimeout != null && idleTimeout.getMillis() > 0) {
            pipeline.addLast("idler",
                    new IdleStateHandler(
                            0,
                            0,
                            idleTimeout.getMillis(),
                            TimeUnit.MILLISECONDS));
        }
        pipeline.addLast("deflater", new HttpContentCompressor());
        pipeline.addLast("handler", new WebServerHandler(getSortedDispatchers()));
    }

    /*
     * Sorts all available dispatchers by their priority ascending
     */
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
