#!/bin/false
# copyright (c) 2011-2012 by andrei borac

makcmd_import("193-ny4j-src",
              {
                "../193-ny4j/export" => /\.(rb|java)$/
              });

$makcmd_java_all_sources << "193-ny4j-src";

makcmd_script("193-ny4j-jar", [ "193-ny4j-src", "057-parts-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-ny4j zs42.ny4j",
              ]);

$makcmd_java_classpath << "jar/zs42-ny4j.jar";
