import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPProxy {
    static int serverPort = 5000;
    static InetAddress serverIp;


    public static void main(String[] args) throws IOException {
        // 启动代理服务器的端口监听
        if (args.length > 0) {
            serverPort = Integer.parseInt(args[0]);
        }
        ServerSocket proxyServer = new ServerSocket(serverPort);
        System.out.println("启动代理服务器成功,监听端口:" + serverPort);
        while (true) {
            new Thread(new Proxy(proxyServer.accept())).start();
            System.out.println("来了一个新的访问");
        }


    }
}
