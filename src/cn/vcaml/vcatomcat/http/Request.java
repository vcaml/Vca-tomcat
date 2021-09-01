package cn.vcaml.vcatomcat.http;

import cn.hutool.core.util.StrUtil;
import cn.vcaml.vcatomcat.catalina.Context;
import cn.vcaml.vcatomcat.catalina.Engine;
import cn.vcaml.vcatomcat.catalina.Service;


import java.io.*;
import java.net.Socket;


/*
* request对象是服务器与浏览器交互的起点，
* 用于存储浏览器请求的主要信息
* */

public class Request extends BaseRequest{

    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;
    private Service service;
    private String method;

    public Request(Socket socket, Service service) throws IOException{
        this.socket = socket;
        this.service = service;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString)) return;
        parseUri();
        parseContext();
        parseMethod();
        if (!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            //访问的地址是 /a, 那么 uri就变成 "" 了，所以考虑这种情况， 让 uri 等于 "/"
            if(StrUtil.isEmpty(uri))
                uri = "/";
        }
    }

    //根据HTTP协议的格式 取第一个空格之前的数据：get/post
    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    //解析http请求
    private void parseHttpRequest() throws IOException {
        InputStream inputStream = this.socket.getInputStream();
        byte[] bytes =readBytes(inputStream,false);
        //对这些字节数组用 UTF-8 字符集进行编码，得到我们要的字符串
        requestString = new String(bytes,"utf-8");

    }
    //读取socket中的流数据
    public static byte[] readBytes(InputStream inputStream,boolean fully) throws IOException {
        //准备一个预长度为1024的缓存
        int bufferSize = 1024;
        byte buffer[] = new byte[bufferSize];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        /*
        * while循环不断的从输入流inputStream读取数据到buffer
        * 注意InputStream是个抽象类 这里的read是自动调用了子类的read方法读取buffer
        * */
        while(true) {
            int length = inputStream.read(buffer);
            if(-1==length)
                break;
            byteArrayOutputStream.write(buffer, 0, length);
            if(!fully && length!=bufferSize)
                break;
        }
        byte[] result =byteArrayOutputStream.toByteArray();
        return result;
    }

    private void parseUri() {
        String tempUri;
        //请求头格式为GET /index.html?name=gareen HTTP/1.1
        //subBetween获取这俩个空格中间的数据：/index.html?name=gareen
        tempUri = StrUtil.subBetween(requestString, " ", " ");
        //检测是否带了参数
        if (!StrUtil.contains(tempUri, '?')) {
            uri = tempUri;
            return;
        }
        tempUri = StrUtil.subBefore(tempUri, '?', false);
        uri = tempUri;
    }

    private void parseContext() {
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if(null!=context)
            return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;

        context = engine.getDefaultHost().getContext(path);
        if (null == context)
            context = engine.getDefaultHost().getContext("/");
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString(){
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }
}
