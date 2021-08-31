package cn.vcaml.vcatomcat.util;

import cn.hutool.core.io.FileUtil;
import cn.vcaml.vcatomcat.catalina.Context;
import cn.vcaml.vcatomcat.catalina.Engine;
import cn.vcaml.vcatomcat.catalina.Host;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/*
*这里引入了jsoup，用于解析和处理xml文件
*/
public class ServerXMLUtil {

    public static List<Context> getContexts() {
        List<Context> resultList = new ArrayList<>();
        //获取 server.xml 的内容
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        //转换为document
        Document document = Jsoup.parse(xml);

        //查询所有的 Context 节点
        Elements elements = document.select("Context");

        //遍历这些节点，并获取对应的 path和docBase ，以生成 Context 对象， 然后放进 result 返回。
        for (Element e : elements) {
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            Context context = new Context(path, docBase);
            resultList.add(context);
        }
        return resultList;
    }

    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element hostElement = d.select("Engine").first();
        return hostElement.attr("defaultHost");
    }

    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        Element hostElement = d.select("Service").first();
        return hostElement.attr("name");
    }

    public static List<Host> getHosts(Engine engine) {
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Host");
        for (Element e : es) {
            String name = e.attr("name");
            Host host = new Host(name,engine);
            result.add(host);
        }
        return result;
    }
}