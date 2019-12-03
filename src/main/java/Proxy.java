import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Proxy implements Runnable {
    static String[] supportProtocols = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"};
    Header header;
    private Socket clientSocket;
    private Socket serverSocket;
    private String requestMsg;
    private String responseMsg;
    private int handleType = 2;// 要使用的转发类型

    public Proxy(Socket clientSocket) {
        this.clientSocket = clientSocket;

    }

    @Override
    public void run() {
        try {
            while (true) {
                // 读取客户端发来的请求
                if (!dealwithRequestFromClient()) {
                    continue;
                }
                if (handleType == 1) {
                    // 方式1:直接将服务器响应转发客户端
                    directForwardingResponse();
                    System.out.println("\n" + Thread.currentThread().getName() + "线程结束操作");
                    return;
                } else {
                    // 方式2:将服务器响应接收,再转发给客户端,可对响应数据进行过滤
                    intrmForwardingResponse();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean dealwithRequestFromClient() throws IOException {
        // 从客户端的连接中读取出请求
        getRequestFromClient();

        // 解析请求中的信息(方法,地址等)
        if (!parseMethod()) {
            return false;
        }

        // 代理服务器把来自客户端的请求转发到目的服务器
        if (!proxyRequestServer()) {
            return false;
        }
        return true;
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
        String[] infos;
        try {
            infos = requestMsg.split("\\r?\\n")[0].split(" ");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.printf("错误msg is:" + requestMsg);
            return false;
        }
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
            System.out.printf("url解析失败\n");
            return false;
        }

        try {
            serverSocket = new Socket(url.getHost(), this.header.port);
        } catch (ConnectException e) {
            System.out.printf("连接目标服务器(%s,%d)失败\n", url.getHost(), this.header.port);
            return false;

        }

        serverSocket.getOutputStream().write(this.requestMsg.getBytes(StandardCharsets.UTF_8));
        serverSocket.getOutputStream().flush();


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
        Thread.sleep(500);
        // todo:先捕获响应头,根据其携带的响应长度content-length信息来接收data
        while ((temp_c = in.read()) != -1) {
            sb.append((char) temp_c);
            if (!in.ready()) {
                break;
            }
        }

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

    /**
     * 先将目标服务器的response存在代理服务器,(经过处理后),再将response返回给请求端
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void intrmForwardingResponse() throws IOException, InterruptedException {
        // 1.代理服务器从目的服务器接收响应信息
        getResponseFromServer();

        // 2.在下面对response进行处理

        // 3.代理服务器将响应信息返回给客户
        returnResponseToClient();
    }


    /**
     * 直接将目标服务器的response发送给请求端
     *
     * @throws IOException
     */
    public void directForwardingResponse() throws IOException {
        // 将用户通过该socket再次发过来的请求也一起转发
        new Thread(new ForwardingRequest(clientSocket.getInputStream(), serverSocket.getOutputStream())).start();

        OutputStream clientOut = clientSocket.getOutputStream();
        InputStream serverIn = serverSocket.getInputStream();
        int c;
        try {
            while ((c = serverIn.read()) != -1) {
                clientOut.write(c);
            }
        } catch (SocketException e) {
            System.out.printf("客户端已经断开连接");
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

    class ForwardingRequest implements Runnable {
        InputStream input;
        OutputStream output;

        public ForwardingRequest(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
            int c;
            try {
                while ((c = input.read()) != -1) {
                    output.write(c);
                }
            } catch (IOException e) {
                System.out.println("远程主机断开连接");
            }
        }
    }
}
