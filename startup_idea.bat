del /q bootstrap.jar
jar cvf0 bootstrap.jar -C out/production/Vcatomcat cn/vcaml/vcatomcat/Bootstrap.class -C out/production/Vcatomcat cn/vcaml/vcatomcat/classloader/CommonClassLoader.class
del /q lib/vcatomcat.jar
cd out
cd production
cd Vcatomcat
jar cvf0 ../../../lib/vcatomcat.jar *
cd ..
cd ..
cd ..
java -cp bootstrap.jar cn.vcaml.vcatomcat.Bootstrap
pause