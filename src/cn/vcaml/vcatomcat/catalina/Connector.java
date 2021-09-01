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
                            HttpProcessor httpProcessor =new HttpProcessor();
                            httpProcessor.execute(socket,request,response);
                        }catch (IOException e) {
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

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }

    //创建一个线程，以当前类为任务，启动运行，并打印 tomcat 风格的日志
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }




}
