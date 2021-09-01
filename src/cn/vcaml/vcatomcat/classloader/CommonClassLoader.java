package cn.vcaml.vcatomcat.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/*
* CommonClassLoader 扫描 lib 目录下的jar, 然后通过 addURL 加到当前的库里面去。
  这样当调用它 的 loadClass 方法的时候，就会从这些 jar 里面去找了。
* */

public class CommonClassLoader extends URLClassLoader {

    public CommonClassLoader() {
        super(new URL[] {});

        try {
            File workingFolder = new File(System.getProperty("user.dir"));
            File libFolder = new File(workingFolder, "lib");
            File[] jarFiles = libFolder.listFiles();
            for (File file : jarFiles) {
                if (file.getName().endsWith("jar")) {
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}