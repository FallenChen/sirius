package sirius.web.http;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

import java.net.InetSocketAddress;

/**
 * Handler for low-level events in the HTTP pipeline.
 * <p>
 * Performs statistical tasks and performs basic filtering based on firewall rules.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
class LowLevelHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
    static LowLevelHandler INSTANCE = new LowLevelHandler();

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            MessageEvent msg = (MessageEvent) e;
            if (msg.getMessage() instanceof ChannelBuffer) {
                WebServer.bytesOut += ((ChannelBuffer) msg.getMessage()).writableBytes();
                if (WebServer.bytesOut < 0) {
                    WebServer.bytesOut = 0;
                }
                WebServer.messagesOut++;
                if (WebServer.messagesOut < 0) {
                    WebServer.messagesOut = 0;
                }
            }
        }
        ctx.sendDownstream(e);
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            if (((ChannelStateEvent) e).getState() == ChannelState.CONNECTED) {
                WebServer.connections++;
                if (WebServer.connections < 0) {
                    WebServer.connections = 0;
                }
                IPRange.RangeSet filter = WebServer.getIPFilter();
                if (!filter.isEmpty()) {
                    if (!filter.accepts(((InetSocketAddress)ctx.getChannel().getRemoteAddress()).getAddress())) {
                        WebServer.blocks++;
                        if (WebServer.blocks < 0) {
                            WebServer.blocks = 0;
                        }
                        ctx.getChannel().close();
                        return;
                    }
                }
            }
        }
        if (e instanceof MessageEvent) {
            MessageEvent msg = (MessageEvent) e;
            if (msg.getMessage() instanceof ChannelBuffer) {
                WebServer.bytesIn += ((ChannelBuffer) msg.getMessage()).readableBytes();
                if (WebServer.bytesIn < 0) {
                    WebServer.bytesIn = 0;
                }
                WebServer.messagesIn++;
                if (WebServer.messagesIn < 0) {
                    WebServer.messagesIn = 0;
                }
            }
        }
        ctx.sendUpstream(e);
    }

}
