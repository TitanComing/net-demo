package netty.echoServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class EchoServer {

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        //设置端口值（如果端口参数的格式不正确，则抛出一个NumberFormatException）
        int port = 6666;
        //调用服务器的 start()方法
        new EchoServer(port).start();
    }

    private void start() throws InterruptedException {
        //这里所有的连接都会使用同一个handler处理
        final EchoServerHandler serverHandler = new EchoServerHandler();
        //1. 创建EventLoopGroup（这个是netty的服务线程，一个EventLoopGroup可以有多个ServerBootstrap，ServerBootstrap中管理了channel,localAddress等）
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            //2. 创建ServerBootstrap
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(eventLoopGroup)
                    //3. 指定通讯channel类型
                    .channel(NioServerSocketChannel.class)
                    //4. 指定socket地址
                    .localAddress(new InetSocketAddress(port))
                    //5. 添加handler到channel上
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(serverHandler);
                        }
                    });
            //6. 创建channel，使用sync()方法阻塞知道其完成
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            log.info("{} started and listening for connections on {}", EchoServer.class.getName(), channelFuture.channel().localAddress());
            //7. 等待channel关闭
            channelFuture.channel().closeFuture().sync();
        } finally {
            //8. 关闭 EventLoopGroup，释放所有的资源
            eventLoopGroup.shutdownGracefully().sync();
        }
    }

}
