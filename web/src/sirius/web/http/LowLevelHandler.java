package sirius.web.http;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/09
 */
public class LowLevelHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
    public static LowLevelHandler INSTANCE = new LowLevelHandler();

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof MessageEvent) {
            MessageEvent msg = (MessageEvent) e;
            if (msg.getMessage() instanceof ChannelBuffer) {
                WebServer.bytesOut += ((ChannelBuffer) msg.getMessage()).readableBytes();
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
            if (((ChannelStateEvent)e).getState() == ChannelState.CONNECTED) {
                // TODO FW
//                InetSocketAddress addr = (InetSocketAddress)ctx.getChannel().getRemoteAddress();
//                addr.getAddress().
                WebServer.connections++;
                if (WebServer.connections < 0) {
                    WebServer.connections = 0;
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
