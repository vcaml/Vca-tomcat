package cn.vcaml.vcatomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.vcaml.vcatomcat.http.Request;
import cn.vcaml.vcatomcat.http.Response;
import cn.vcaml.vcatomcat.servlets.DefaultServlet;
import cn.vcaml.vcatomcat.servlets.InvokerServlet;
import cn.vcaml.vcatomcat.util.Constant;
import cn.vcaml.vcatomcat.util.SessionManager;
import cn.vcaml.vcatomcat.util.WebXMLUtil;
import cn.vcaml.vcatomcat.webappservlet.HelloServlet;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor {
    public void execute(Socket socket, Request request, Response response){
        try {
            String uri = request.getUri();
            if (null == uri)
                return;
            //准备session, 先通过 cookie拿到 jsessionid, 然后通过 SessionManager 创建 session, 并且设置在 requeset 上
            prepareSession(request, response);

            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);
            System.out.println("name1:"+servletClassName);
            if(null!=servletClassName)
                InvokerServlet.getInstance().service(request,response);
            else
                DefaultServlet.getInstance().service(request,response);

            if(Constant.CODE_200 == response.getStatus()){
                handle200(socket, response);
                return;
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(socket, uri);
                return;
            }

        } catch (Exception e) {
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

    public void prepareSession(Request request, Response response) {
        String jsessionid = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        request.setSession(session);
    }



}
