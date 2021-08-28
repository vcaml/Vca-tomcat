package cn.vcaml.vcatomcat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import cn.vcaml.vcatomcat.http.Request;
import cn.vcaml.vcatomcat.http.Response;
import cn.vcaml.vcatomcat.http.ThreadPoolUtil;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//
public class Bootstrap {

    public static void main(String[] args) {
        logJVM();
        try {
            int port = 18080;
            //在端口18080上启动 ServerSocket。 服务端和浏览器通信是通过 Socket进行通信的，所以这里需要启动一个 ServerSocket
            ServerSocket serverSocket = new ServerSocket(port);

            while(true) {
                Socket socket =  serverSocket.accept();
                Runnable runnable= new Runnable(){
                    @Override
                    public void run(){
                       try {
                           Request request = new Request(socket);
                           System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                           System.out.println("uri:" + request.getUri());

                           Response response = new Response();
                           String uri = request.getUri();
                           if(null==uri)
                               return;
                           if("/".equals(uri)) {
                               String html = "Start to VcaTomcat";
                               response.getWriter().println(html);
                           }else{
                               // 获取后缀文件名
                               String fileName = StrUtil.removePrefix(uri, "/");
                               //找指定文件夹去寻找文件
                               File file = FileUtil.file(Constant.rootFolder,fileName);
                               if(file.exists()){
                                   String fileContent = FileUtil.readUtf8String(file);
                                   response.getWriter().println(fileContent);

                                   if(fileName.equals("timeConsume.html")){
                                       //这里为了模仿耗时任务故意等1s
                                       ThreadUtil.sleep(1000);
                                   }
                               }
                               else{
                                   response.getWriter().println("File Not Found");
                               }
                           }
                           handle200(socket, response);
                       }catch (IOException e){
                           e.printStackTrace();
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


    private static void handle200(Socket socket, Response response) throws IOException {
        //从respond中获取contentType信息
        String contentType = response.getContentType();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType);

        byte[] head = headText.getBytes();
        //html文件中的响应内容body存入response 这次再取出来
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

    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "vcaml vcaTomcat/1.0.1");
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

}