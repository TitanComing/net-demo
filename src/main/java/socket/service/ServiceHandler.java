package socket.service;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServiceHandler extends Thread {
    Socket sock;

    public ServiceHandler(Socket sock) {
        this.sock = sock;
    }

    @Override
    public void run() {
        try (InputStream input = this.sock.getInputStream()) {
            try (OutputStream output = this.sock.getOutputStream()) {
                handle(input, output);
            }
        } catch (Exception e) {
            try {
                this.sock.close();
            } catch (IOException ioe) {
            }
            System.out.println("client disconnected.");
        }
    }

    private void handle(InputStream input, OutputStream output) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        writer.write("connected to demo socket service(type bye to disconnect):...\n");
        writer.flush();
        for (;;) {
            String s = reader.readLine();
            System.out.println("server get req: " + s);
            if (s.equals("bye")) {
                writer.write("bye\n");
                writer.flush();
                break;
            }
            writer.write("ok: " + s + "\n");
            writer.flush();
            System.out.println("server put resp: " + "ok: " + s + "\n");
        }
    }
}
