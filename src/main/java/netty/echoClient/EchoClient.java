package netty.echoClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class EchoClient {
    private final String host;
    private final int port;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException {
        final String host = "localhost";
        final int port = 6666;
        new EchoClient(host, port).start();
    }


    private void start() throws InterruptedException {
        //创建eventLoopGroup服务线程
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            //创建bootstrap
            Bootstrap bootstrap = new Bootstrap();
            //设置bootstrap
            bootstrap.group(eventLoopGroup)
                    //设置bootstrap的channel类型
                    .channel(NioSocketChannel.class)
                    //设置bootstrap的远程地址
                    .remoteAddress(new InetSocketAddress(host, port))
                    //设置bootstrap的handler
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new EchoClientHandler());
                        }
                    });
            //连接到远程节点，阻塞等待直到连接完成
            ChannelFuture channelFuture = bootstrap.connect().sync();
            //阻塞，直到Channel 关闭
            channelFuture.channel().closeFuture().sync();
        } finally {
            //关闭线程池并且释放所有的资源
            eventLoopGroup.shutdownGracefully().sync();
        }
    }

}
