#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("077-pixels-codec-src",
              {
                "../077-pixels-codec/export" => /.(java)$/
              });

$makcmd_java_all_sources << "077-pixels-codec-src";

makcmd_script("077-pixels-codec-jar", [ "077-pixels-codec-src", "054-mass-jar", "086-buff-jar", "098-nats-codec-jar" ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-pixels-codec zs42.pixels.codec"
              ]);

$makcmd_java_classpath << "jar/zs42-pixels-codec.jar";

makcmd_script("077-export", [ "054-mass-jar", "086-buff-jar", "098-nats-codec-jar", "077-pixels-codec-jar" ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p export",
               "echo \"export CLASSPATH='\$CLASSPATH'\" > export/java_classpath",
               "cp -r jar export"
              ]);
