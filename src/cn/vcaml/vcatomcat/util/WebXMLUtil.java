package cn.vcaml.vcatomcat.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cn.vcaml.vcatomcat.catalina.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cn.hutool.core.io.FileUtil;

import static cn.vcaml.vcatomcat.util.Constant.webXmlFile;

public class WebXMLUtil {
    //文件后缀名 和mimeType 一一对应
    private static Map<String, String> mimeTypeMapping = new HashMap<>();

    //这里加了锁  因为会调用 initMimeType 进行初始化，如果两个线程同时来，那么可能导致被初始化两次。
    public static synchronized String getMimeType(String extName) {
        if (mimeTypeMapping.isEmpty())
            initMimeType();

        String mimeType = mimeTypeMapping.get(extName);
        if (null == mimeType)
            return "text/html";

        return mimeType;
    }

    private static void initMimeType() {
        String xml = FileUtil.readUtf8String(webXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("mime-mapping");
        for (Element e : es) {
            String extName = e.select("extension").first().text();
            String mimeType = e.select("mime-type").first().text();
            mimeTypeMapping.put(extName, mimeType);
        }

    }
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("welcome-file");
        for (Element e : es) {
            String welcomeFileName = e.text();
            File f = new File(context.getDocBase(), welcomeFileName);
            if (f.exists())
                return f.getName();
        }
        return "index.html";
    }
}