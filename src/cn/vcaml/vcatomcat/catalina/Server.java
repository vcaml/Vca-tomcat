package cn.vcaml.vcatomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Server {

    private Service service;
    public Server(){
        this.service = new Service(this);
    }

    public void start(){
        logJVM();
        init();
    }

    private void init() {
        try {
            /*
            ( 已被重构纳入到 host对象中  由engine封装 )
            在初始启动时 自动扫描webapps下面所有文件，将（文件路径，context对象）插入map中 :scanContextsOnWebAppsFolder();
            配置型多应用 :scanContextsInServerXML();
            *
            */
            int port = 18080;
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

    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "How2J DiyTomcat/1.0.1");
        infos.put("Server built", "2020-04-08 10:20:22");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key+":\t\t" + infos.get(key));
        }
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
