#!/bin/bash
BASE_DIR="$(dirname "$0")/.."

# platform ops
P_OPTS=

case `uname` in
   Darwin*)
   P_OPTS="-Xdock:name=Gutool -Xdock:icon=$BASE_DIR/icns/gutool.icns -Dapple.laf.useScreenMenuBar=true -Dapple.eawt.quitStrategy=CLOSE_ALL_WINDOWS"
   ;;
esac


java -Dswing.aatext=true \
-XX:+UseZGC \
-XX:+ZGenerational \
-XX:-ZUncommit \
-Xms128m \
-Xmx512m \
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
--add-opens java.base/java.lang=ALL-UNNAMED \
-jar $P_OPTS \
"$BASE_DIR/gutool.jar" "$@"
