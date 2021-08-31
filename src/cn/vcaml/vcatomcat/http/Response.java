package cn.vcaml.vcatomcat.http;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

/*
* response对象是服务器与浏览器交互的末尾，
* 用于存储服务器响应的主要信息
* */

public class Response {

    //用于存放返回的 html 文本
    private StringWriter stringWriter;
    private PrintWriter writer;

    //contentType就是对应响应头信息里的 Content-type ，默认是 "text/html"
    private String contentType;

    public Response(){
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public byte[] getBody() throws UnsupportedEncodingException {
        String content = stringWriter.toString();
        byte[] body = content.getBytes("utf-8");
        return body;
    }
}
