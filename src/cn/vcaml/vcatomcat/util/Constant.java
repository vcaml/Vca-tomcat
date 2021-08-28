package cn.vcaml.vcatomcat.util;

import cn.hutool.system.SystemUtil;

import java.io.File;

//常量工具类，用于存放响应的头信息模板。
public class Constant {
    public final static String response_head_202 = "HTTP/1.1 200 OK\r\n" + "Content-Type: {}\r\n\r\n";
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"),"webapps");
    public final static File rootFolder = new File(webappsFolder,"ROOT");
}
