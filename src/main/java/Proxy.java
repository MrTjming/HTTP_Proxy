import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Proxy implements Runnable {
    static String[] supportProtocols = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"};
    Header header;
    private Socket clientSocket;
    private Socket serverSocket;
    private String requestMsg;
    private String responseMsg;

    public Proxy(Socket clientSocket) {
        this.clientSocket = clientSocket;

    }

    /**
     * @return 从客户端读取请求信息
     * @throws IOException
     */
    public boolean getRequestFromClient() throws IOException {
        String tempStr;
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        while (true) {
            tempStr = in.readLine();
            if (tempStr == null || "".equals(tempStr)) {
                break;
            }
            sb.append(tempStr + "\n");
        }
        // 在消息尾部加上空行，告诉服务器消息结束了
        sb.append("\n");

        requestMsg = sb.toString();
        return true;
    }

    /**
     * 解析出方法
     */
    public boolean parseMethod() {

        String[] infos = requestMsg.split("\\r?\\n")[0].split(" ");
        String method = infos[0];
        //todo: 判断方法是否被支持

        String addr = infos[1];
        int port = 80;

        String pattern = ":\\d+";
        if (Pattern.matches(pattern, infos[1])) {
            int index = infos[1].lastIndexOf(":");
            addr = infos[1].substring(0, index);
            port = Integer.parseInt(infos[1].substring(index + 1));
            if (port != 80) {
                return false;
            }
        }
        header = new Header(method, addr, port, infos[2]);
        return true;
    }

    /**
     * @return 代理客户端发送请求到服务器
     */
    public boolean proxyRequestServer() throws IOException {
        URL url;
        try {
            url = new URL(this.header.addr);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            return false;
        }

        serverSocket = new Socket(url.getHost(), this.header.port);
        serverSocket.getOutputStream().write(this.requestMsg.getBytes(StandardCharsets.UTF_8));
        serverSocket.getOutputStream().flush();
//        serverSocket.shutdownOutput();

        return true;
    }

    /**
     * @return 从服务器中取得响应
     * @throws IOException
     */
    public boolean getResponseFromServer() throws IOException, InterruptedException {

        StringBuilder sb = new StringBuilder();
        String tempStr;
        System.out.println("开始接收服务器的响应 {" + this.header.addr);
        BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        int temp_c;
        while ((temp_c = in.read()) != -1 && in.ready()) {
            sb.append((char) temp_c);
        }
        sb.append((char) temp_c);
//        while(true) {
//            tempStr = in.readLine();
//            System.out.println("收到:"+tempStr);
//            if (tempStr==null){
//                break;
//            }else if(tempStr.equals("")){
//                System.out.println("读到一个空行");
//            }
//            sb.append(tempStr).append("\n");
//        }


        responseMsg = sb.toString();
        System.out.printf("线程：%s，长度：%d,url:%s\n", Thread.currentThread().getName(), responseMsg.length(), this.header.addr);
        return true;
    }

    /**
     * @return 代理服务器将响应信息返回给客户
     * @throws IOException
     */
    public boolean returnResponseToClient() throws IOException {
        // 代理服务器将响应信息返回给客户
        clientSocket.getOutputStream().write(responseMsg.getBytes(StandardCharsets.UTF_8));
        clientSocket.getOutputStream().flush();
        clientSocket.close();
        return true;
    }

    @Override
    public void run() {
        try {
            //while (true){
            // 从客户端的连接中读取出请求
            getRequestFromClient();

            // 解析请求中的信息(方法,地址等)
            if (!parseMethod()) {
                return;
            }

            // 代理服务器把来自客户端的请求转发到目的服务器
            proxyRequestServer();

            // 代理服务器从目的服务器接收响应信息
            getResponseFromServer();

            // 代理服务器将响应信息返回给客户
            returnResponseToClient();
            System.out.println("\n" + Thread.currentThread().getName() + "线程结束操作");
            // }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    class Header {
        String method;
        String addr;
        int port;
        String protocol;

        public Header(String method, String addr, int port, String protocol) {
            this.method = method;
            this.addr = addr;
            this.port = port;
            this.protocol = protocol;
        }
    }
}
