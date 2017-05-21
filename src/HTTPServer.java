import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(82);
        while (true) {
            Socket client = server.accept();
            new Processor(client);
        }
    }
}
