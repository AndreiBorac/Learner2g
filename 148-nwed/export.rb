#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("148-nwed-src",
              {
                "../148-nwed/export" => /.(java)$/
              });

$makcmd_java_all_sources << "148-nwed-src";

makcmd_script("148-nwed-jar", [ "148-nwed-src" ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-nwed zs42.nwed"
              ]);

$makcmd_java_classpath << "jar/zs42-nwed.jar";
