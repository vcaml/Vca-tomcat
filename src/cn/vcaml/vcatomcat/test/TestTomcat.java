package cn.vcaml.vcatomcat.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.vcaml.vcatomcat.util.MiniBrowser;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class TestTomcat {
    private static int port = 18080;
    private static String ip = "127.0.0.1";

    @BeforeClass
    public static void beforeClass() {
        //所有测试开始前看diy tomcat 是否已经启动了
        if(NetUtil.isUsableLocalPort(port)) {
            System.err.println("请先启动 位于端口: " +port+ " 的diy tomcat，否则无法进行单元测试");
            System.exit(1);
        }
        else {
            System.out.println("检测到 vcaml tomcat已经启动，开始进行单元测试");
        }
    }

    @Test
    public void testHelloTomcat() {
        String html = getContentString("/");
        System.out.println(html);
        Assert.assertEquals(html,"Start to VcaTomcat");
    }

    @Test
    public void testaTxt() {
        String response  = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }


    @Test
    public void testbHtml() {
        String html = getContentString("/a.txt");
        System.out.println(html);
        Assert.assertEquals(html,"Start to VcaTomcat form html");
    }

    @Test
    public void testaHtml() {
        String html = getContentString("/a/index.html");
        System.out.println(html);
        Assert.assertEquals(html,"TEST:Start to VcaTomcat form html from Folder-a index.html@a");
    }


    @Test
    public void testbIndex() {
        String html = getContentString("/b/index.html");
        System.out.println(html);
        Assert.assertEquals(html,"TEST:Start to VcaTomcat form html from Folder-a-b index.html@a");
    }

    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        //构建一个计数器 数量为3
        CountDownLatch countDownLatch=new CountDownLatch(3);
        TimeInterval timeInterval= DateUtil.timer();
        for (int i = 0; i <3; i++) {
            new Thread(()->{
                try {
                    String html= getContentString("/timeConsume.html");
                    System.out.println(html);
                    //　此函数将递减锁存器的计数，如果计数到达零，则释放所有等待的线程　
                    countDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },"Thread "+i).start();
        }
        // await 此函数将会使当前线程在锁存器倒计数至零之前一直等待，除非线程被中断
        countDownLatch.await();
        long duration=timeInterval.intervalMs();
        System.out.println("一共耗时为："+duration);
        //毫秒是一种较为微小的时间单位，符号为ms，1秒 =1000 毫秒
        Assert.assertTrue(duration<3000);
    }

    @Test
    public void test404() {
        String response  = getHttpString("/not_exist.html");
        containAssert(response, "HTTP/1.1 404 Not Found");
    }


    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        System.out.println("初始url"+url);
        String content = MiniBrowser.getContentString(url);
        return content;
    }

    private String getHttpString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http = MiniBrowser.getHttpString(url);
        return http;
    }

    private void containAssert(String html, String string) {
        boolean match = StrUtil.containsAny(html, string);
        Assert.assertTrue(match);
    }
}
