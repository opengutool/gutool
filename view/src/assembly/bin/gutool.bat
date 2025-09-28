@echo off
set BASE_DIR=%~dp0..

java -Dswing.aatext=true -XX:+UseZGC -XX:+ZGenerational -XX:-ZUncommit -Xms128m -Xmx512m ^
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED ^
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED ^
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED ^
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED ^
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED ^
--add-opens java.base/java.lang=ALL-UNNAMED ^
-jar "%BASE_DIR%\gutool.jar" %*
