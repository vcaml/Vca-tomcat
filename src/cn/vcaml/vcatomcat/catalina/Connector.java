package cn.vcaml.vcatomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.vcaml.vcatomcat.http.Request;
import cn.vcaml.vcatomcat.http.Response;
import cn.vcaml.vcatomcat.http.ThreadPoolUtil;
import cn.vcaml.vcatomcat.util.Constant;
import cn.vcaml.vcatomcat.util.WebXMLUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable {

    int port;
    private Service service;
    public Connector(Service service) { this.service = service; }

    public Service getService() { return service; }

    public void setPort(int port) { this.port = port; }

    @Override
    public void run() {
        try {
            //在端口18080上启动 ServerSocket。 服务端和浏览器通信是通过 Socket进行通信的，所以这里需要启动一个 ServerSocket
            ServerSocket serverSocket = new ServerSocket(port);

            while (true) {
                Socket socket = serverSocket.accept();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Request request = new Request(socket, service);
                            Response response = new Response();
                            String uri = request.getUri();
                            if (null == uri)
                                return;
                            Context context = request.getContext();
                            if("/500.html".equals(uri)){
                                throw new RuntimeException("this is a deliberately created exception");
                            }

                            if("/".equals(uri))
                                uri = WebXMLUtil.getWelcomeFile(request.getContext());

                            // 获取后缀文件名
                            String fileName = StrUtil.removePrefix(uri, context.getPath());
                            // 根据文件名 获取文件完整路径
                            File file = FileUtil.file(context.getDocBase(), fileName);

                            if (file.exists()) {
                                String extName = FileUtil.extName(file);
                                String mimeType = WebXMLUtil.getMimeType(extName);
                                response.setContentType(mimeType);

//                                    String fileContent = FileUtil.readUtf8String(file);
//                                    response.getWriter().println(fileContent);
                                byte body[] = FileUtil.readBytes(file);
                                response.setBody(body);

                                if (fileName.equals("timeConsume.html")) {
                                    //这里为了模仿耗时任务故意等1s
                                    ThreadUtil.sleep(1000);
                                }
                            } else {
                                handle404(socket, uri);
                                return;
                            }

                            handle200(socket, response);
                        } catch (IOException e) {
                            LogFactory.get().error(e);
                            handle500(socket,e);
                        } finally {
                            try {
                                if (!socket.isClosed())
                                    socket.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                ThreadPoolUtil.run(runnable);
            }
        } catch (IOException e) {
            e.printStackTrace();
            LogFactory.get().error(e);
        }


    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }

    //创建一个线程，以当前类为任务，启动运行，并打印 tomcat 风格的日志
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }

    private static void handle200(Socket s, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType);
        byte[] head = headText.getBytes();

        byte[] body = response.getBody();

        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
    }

    protected void handle404(Socket s, String uri) throws IOException {
        OutputStream os = s.getOutputStream();
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes("utf-8");
        os.write(responseByte);
    }

    protected void handle500(Socket s, Exception e) {
        try {
            OutputStream os = s.getOutputStream();
            // 拿到 Exception 的异常堆栈，比如平时我们看到一个报错，
            // 都会打印最哪个类的哪个方法，依次调用过来的信息。
            // 这个信息就放在这个 StackTrace里，
            // 是个 StackTraceElement 数组。
            StackTraceElement stes[] = e.getStackTrace();
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement ste : stes) {
                sb.append("\t");
                sb.append(ste.toString());
                sb.append("\r\n");
            }

            String msg = e.getMessage();

            if (null != msg && msg.length() > 20)
                msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), sb.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes("utf-8");
            os.write(responseBytes);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }



}
