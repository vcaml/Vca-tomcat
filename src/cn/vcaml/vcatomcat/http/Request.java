package cn.vcaml.vcatomcat.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.vcaml.vcatomcat.catalina.Context;
import cn.vcaml.vcatomcat.catalina.Engine;
import cn.vcaml.vcatomcat.catalina.Service;


import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;


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
    private String queryString;
    private Map<String, String[]> parameterMap;
    private Map<String, String> headerMap;
    private Cookie[] cookies;
    private HttpSession session;

    public Request(Socket socket, Service service) throws IOException{
        this.headerMap = new HashMap<>();
        this.parameterMap = new HashMap();
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
        //从requestString中解析出get/post参数
        parseParameters();
        parseHeaders();
        parseCookies();
        System.out.println(headerMap);
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

    private void parseParameters() {
        System.out.println(requestString);
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " ", " ");
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString || 0==queryString.length())
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                String values[] = parameterMap.get(name);
                if (null == values) {
                    values = new String[] { value };
                    parameterMap.put(name, values);
                } else {
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headerMap.put(headerName, headerValue);
            // System.out.println(line);
        }
    }

    /*
    重写父类 request相关的方法
    */

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }
    public Map getParameterMap() {
        return parameterMap;
    }
    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }
    public Enumeration getHeaderNames() {
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }
    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }


    /*
    request其他常见方法
     */

    public String getLocalAddr() {

        return socket.getLocalAddress().getHostAddress();
    }

    public String getLocalName() {

        return socket.getLocalAddress().getHostName();
    }

    public int getLocalPort() {

        return socket.getLocalPort();
    }
    public String getProtocol() {

        return "HTTP:/1.1";
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();

        return StrUtil.subAfter(temp, "/", false);

    }

    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();

    }

    public int getRemotePort() {
        return socket.getPort();
    }
    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return getHeader("host").trim();
    }

    public int getServerPort() {
        return getLocalPort();
    }
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }
    public String getRequestURI() {
        return uri;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());

        return url;
    }
    public String getServletPath() {
        return uri;
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    //从 http 请求协议中解析出 Cookie
    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    public HttpSession getSession() {
        return session;
    }
    public void setSession(HttpSession session) {
        this.session = session;
    }

    public String getJSessionIdFromCookie() {
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
