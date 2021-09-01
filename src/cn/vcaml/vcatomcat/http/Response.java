package cn.vcaml.vcatomcat.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;


public class Response extends BaseResponse {

    //用于存放返回的 html 文本
    private StringWriter stringWriter;
    private PrintWriter writer;
    //contentType就是对应响应头信息里的 Content-type ，默认是 "text/html"
    private String contentType;
    //准备一个 body[] 来存放二进制文件。
    private byte[] body;
    private int status;

    public Response() {
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

    //修改 getter, 当body 不为空的时候，直接返回 body
    public byte[] getBody() throws UnsupportedEncodingException {
        if (null == body) {
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }
    @Override
    public int getStatus() {
        return status;
    }

}