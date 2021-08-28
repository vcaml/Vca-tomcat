package cn.vcaml.vcatomcat;

import cn.hutool.core.io.FileUtil;
import cn.vcaml.vcatomcat.http.Request;
import cn.vcaml.vcatomcat.http.Response;
import cn.vcaml.vcatomcat.util.Constant;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Bootstrap {

    public static void main(String[] args) {
          //http://127.0.0.1:18080

        try {
            int port = 18080;
            if(!NetUtil.isUsableLocalPort(port)) {
                System.out.println(port +" 端口已经被占用了");
                return;
            }

            //在端口18080上启动 ServerSocket。 服务端和浏览器通信是通过 Socket进行通信的，所以这里需要启动一个 ServerSocket
            ServerSocket serverSocket = new ServerSocket(port);

            while(true) {
                Socket socket =  serverSocket.accept();
                Request request = new Request(socket);
                System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                System.out.println("uri:" + request.getUri());

                Response response = new Response();
                String uri = request.getUri();
                if(null==uri)
                    continue;
                System.out.println(uri);

                if("/".equals(uri)) {
                    String html = "Start to VcaTomcat";
                    response.getWriter().println(html);
                }else{
                    String fileName = StrUtil.removePrefix(uri, "/");
                    File file = FileUtil.file(Constant.rootFolder,fileName);
                    if(file.exists()){
                        String fileContent = FileUtil.readUtf8String(file);
                        response.getWriter().println(fileContent);
                    }
                    else{
                        response.getWriter().println("File Not Found");
                    }
                }
                handle200(socket, response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void handle200(Socket socket, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType);

        byte[] head = headText.getBytes();
        byte[] body = response.getBody();
        byte[] responseBytes = new byte[head.length + body.length];
        //拷贝请求头的数据到新数组,是左开右闭的
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        //拷贝请求体的数据到新数组
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream os = socket.getOutputStream();
        os.write(responseBytes);
        socket.close();
    }
}