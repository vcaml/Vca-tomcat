package cn.vcaml.vcatomcat.servlets;

import cn.hutool.core.util.ReflectUtil;
import cn.vcaml.vcatomcat.catalina.Context;
import cn.vcaml.vcatomcat.http.Request;
import cn.vcaml.vcatomcat.http.Response;
import cn.vcaml.vcatomcat.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InvokerServlet extends HttpServlet {

    private static InvokerServlet instance = new InvokerServlet();

    /*
    这里设计为单例模式 原因是创建对象会有开销，单例模式下不需要每次创建新的对象
    理论上可以不加同步锁，但是为了保证线程安全 这里还是加上了
     */
    public static synchronized InvokerServlet getInstance() {
        return instance;
    }

    private InvokerServlet() {

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        Object servletObject = ReflectUtil.newInstance(servletClassName);
        ReflectUtil.invoke(servletObject, "service", request, response);

        //因为目标 servlet 实现了 HttpServlet ,所以一定提供了 service 方法。
        // 这个 service 方法实会根据 request 的 Method ，访问对应的 doGet 或者 doPost。

        //表示返回成功
        response.setStatus(Constant.CODE_200);
    }

}
