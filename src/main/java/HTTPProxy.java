import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class HTTPProxy {
    static int serverPort = 5000;
    static InetAddress serverIp;


    public static void main(String[] args) throws IOException {
        // 启动代理服务器的端口监听
        ServerSocket proxyServer = new ServerSocket(serverPort);
        System.out.println("启动代理服务器成功,监听端口:" + serverPort);
        while (true) {
            Socket clientSocket = proxyServer.accept();
            System.out.println("来了一个新的访问");
            new Thread(new Proxy(clientSocket)).start();

        }


    }
}
