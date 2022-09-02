package netty.echoServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        //记录收到的消息
        log.info("Server received: {}", in.toString(CharsetUtil.UTF_8));
        //将接收到的消息处理后给发送者
        String echo = "OK: " + in.toString(CharsetUtil.UTF_8);
        ByteBuf out =  ctx.alloc().buffer(4 * echo.length());
        out.writeBytes(echo.getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(out);

        //释放资源
        in.release();
        out.release();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //接收消息完成后，将消息推送回去，并且推送完成后关闭channel
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        log.info("Server close channel ");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //出现异常记录
        log.error("Server exception: {}", cause.toString());
        //关闭channel
        ctx.close();
    }
}
