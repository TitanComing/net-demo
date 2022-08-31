package socket.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceSocket {
    public static void main(String[] args) throws IOException {
        int serverPort = 6666;
        ServerSocket ss = new ServerSocket(serverPort); // 监听指定端口
        System.out.println("socket server is running at port: " + serverPort);
        for (;;) {
            Socket sock = ss.accept();
            System.out.println("connected from " + sock.getRemoteSocketAddress());
            Thread t = new ServiceHandler(sock);
            t.start();
        }
    }
}
