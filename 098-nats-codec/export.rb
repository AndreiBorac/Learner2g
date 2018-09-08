#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("098-nats-codec-jni-src",
              {
                "../098-nats-codec/export" => /(Optimizer\.java|optimizer.cpp)$/
              });

makcmd_script("098-nats-codec-jni-lib", [ "098-nats-codec-jni-src" ],
              [
               "set -x",
               "mkdir -p ../temp/classes ../temp/codegen",
               "javac -d ../temp/classes java/zs42/nats/codec/Optimizer.java",
               "javah -d ../temp/codegen -classpath ../temp/classes zs42.nats.codec.Optimizer",
               # "cat ../temp/codegen/*.h",
               "JAVA_HOME=\"`which javah`\"",
               "JAVA_HOME=\"`readlink -f \$JAVA_HOME`\"",
               "JAVA_HOME=\"\${JAVA_HOME%/bin/javah}\"",
               "export PATH=\"\$PATH:/usr/lib/`gcc -dumpmachine`/gcc/`gcc -dumpversion`\"",
               "echo \"PATH='\$PATH'\"",
               "gcc -x c -std=c99 -O3 -Werror -Wall -I/usr/lib/gcc/`gcc -dumpmachine`/`gcc -dumpversion`/include -I\$JAVA_HOME/include -I\$JAVA_HOME/include/linux -fPIC -c -o ../temp/optimizer.o cpp/optimizer.cpp -DUSECRITICAL",
               "mkdir -p ../root/so",
               "ld -shared -o ../root/so/libzs42natc.so ../temp/optimizer.o"
              ]);

makcmd_import("098-nats-codec-src",
              {
                "../098-nats-codec/export" => /\.(java)$/
              });

$makcmd_java_all_sources << "098-nats-codec-src";

makcmd_script("098-nats-codec-jar", [ "098-nats-codec-src", "054-mass-jar", "086-buff-jar" ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-nats-codec zs42.nats.codec"
              ]);

$makcmd_java_classpath << "jar/zs42-nats-codec.jar";

makcmd_script("098-nats-codec-export", [ "054-mass-jar", "086-buff-jar", "098-nats-codec-jar", "098-nats-codec-jni-lib" ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p export",
               "echo \"export CLASSPATH='\$CLASSPATH'\" > export/java_classpath",
               "cp -r so jar export"
              ]);
