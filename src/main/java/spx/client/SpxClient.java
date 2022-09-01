package spx.client;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/***
 * spx网关调用客户端
 * 金仕达交易的spx网关不需要客户端登陆认证；新意账户系统的spx网关需要登陆认证
 *
 * rp
 * 20220901
 */
@Slf4j
public class SpxClient {

    /*缓冲区大小*/
    private static final int BLOCK = 4096;
    /*接受数据缓冲区*/
    private static final ByteBuffer SEDN_BUFFER = ByteBuffer.allocate(BLOCK);
    /*发送数据缓冲区*/
    private static final ByteBuffer RECEIVE_BUFFER = ByteBuffer.allocate(BLOCK);
    /*连接超时时间（毫秒）*/
    private static final int CONNECT_TIMEOUT = 1000;
    /*服务器端地址*/
    private static InetSocketAddress SERVER_ADDRESS;

    /*socket连接通道和选择器*/
    private SocketChannel socketChannel;
    private Selector selector;
    /*是否已验证账号*/
    private boolean logon = false;

    public SpxClient(String ip, int port) {
        SERVER_ADDRESS = new InetSocketAddress(ip, port);
    }

    /**
     * 客户端连接到spx网关
     */
    public boolean connect() throws IOException {
        // 创建socket通道
        socketChannel = SocketChannel.open();
        // 设置为非阻塞方式
        socketChannel.configureBlocking(false);
        // 创建选择器
        selector = Selector.open();
        // socket通道绑定连接选择器
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        // 注册连接服务端socket动作
        socketChannel.connect(SERVER_ADDRESS);

        Set<SelectionKey> selectionKeys;
        Iterator<SelectionKey> iterator;
        SelectionKey selectionKey;
        //等待连接最大超时时间
        int keyNum = selector.select(CONNECT_TIMEOUT);
        if (keyNum > 0) {
            // 选择器就绪
            selectionKeys = selector.selectedKeys();
            iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                selectionKey = iterator.next();
                if (selectionKey.isConnectable()) {
                    // 判断此通道上是否正在进行连接操作。
                    if (socketChannel.isConnectionPending()) {
                        // 完成套接字通道的连接过程。
                        socketChannel.finishConnect();
                        log.info("spx网关连接成功......");
                        return true;
                    }
                }
            }
        }

        log.warn("spx网关连接失败......");
        return false;
    }

    /**
     * 写入数据
     * 入参：数据内容，写入超时时间（毫秒）
     */
    public void write(String data, long millis) throws IOException {
        //注册监听写操作
        registerWrite();
        //设置超时等待时间
        int keyNum = selector.select(millis);
        if (keyNum > 0) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey selectionKey : selectionKeys) {
                //写操作是否准备就续
                if (selectionKey.isWritable()) {
                    SEDN_BUFFER.clear();
                    SEDN_BUFFER.put(data.getBytes());
                    //将内存缓存区域从写模式转变为读模式，从而可以将其写入socketChannel
                    SEDN_BUFFER.flip();
                    socketChannel.write(SEDN_BUFFER);
                    break;
                }
            }
            selectionKeys.clear();
        }
    }

    /**
     * 读取数据
     * 入参：读取超时时间（毫秒）
     */
    public String read(long millis) throws IOException {
        StringBuilder receiveText = new StringBuilder();
        //注册读取监听操作
        registerRead();
        //设置超时等待时间
        int keyNum = selector.select(millis);
        if (keyNum > 0) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey selectionKey : selectionKeys) {
                //读操作是否准备就续
                if (selectionKey.isReadable()) {
                    RECEIVE_BUFFER.clear();
                    //读取服务器发送来的数据到缓冲区中
                    int count = 0;
                    while ((count = socketChannel.read(RECEIVE_BUFFER)) > 0) {
                        //金仕达的spx网关默认是GBK编码，不指定编码模式可能中文乱码
                        receiveText.append(new String(RECEIVE_BUFFER.array(), 0, count, Charset.forName("GBK")));
                        RECEIVE_BUFFER.clear();
                    }
                    break;
                }
            }
            selectionKeys.clear();
        }
        return receiveText.toString();
    }

    public boolean isOpen() {
        return socketChannel.isOpen();
    }

    public void close() {
        try {
            if (selector != null) {
                selector.close();
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException e) {
            log.warn("spx网关连接socket连接关闭异常....");
        }
    }

    public boolean isLogon() {
        return logon;
    }

    public void setLogon(boolean logon) {
        this.logon = logon;
    }

    private void registerRead() throws ClosedChannelException {
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void registerWrite() throws ClosedChannelException {
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    public static void main(String[] args) throws Exception {
        String output = "R|00b0d07abcb0|01|36|003|1| |00181215||1|";
        final SpxClient client = new SpxClient("10.46.32.13", 17991);

        if (client.connect()) {
            log.info("重新连接到spx网关......");
            try {
                client.write(output, 2000);
                log.info(">>> {}",output);
                String resp = client.read(2000);
                log.info("<<< {}",resp);
            } catch (IOException e) {
                log.warn("spx网关调用异常： {}",e.getMessage());
            }
        }

        if(client.isOpen()){
            log.info("已经连接到了spx网关......");
            try {
                client.write(output, 2000);
                log.info(">>> {}",output);
                String resp = client.read(2000);
                log.info("<<< {}",resp);
            } catch (IOException e) {
                log.warn("spx网关调用异常： {}",e.getMessage());
            }
        }

        if(client.isOpen()){
            client.close();
            log.info("关闭了对spx网关的连接....");
        }

    }
}
