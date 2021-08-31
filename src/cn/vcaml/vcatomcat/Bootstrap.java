package cn.vcaml.vcatomcat;
import cn.vcaml.vcatomcat.catalina.Server;

public class Bootstrap {

    public static void main(String[] args) {
            Server server  = new Server();
            server.start();
    }

}
