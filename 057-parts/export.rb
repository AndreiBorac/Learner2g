#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("057-parts-src",
              {
                "../057-parts/export" => /.(java)$/
              });

$makcmd_java_all_sources << "057-parts-src";

makcmd_script("057-parts-jar", [ "057-parts-src" ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages parts zs42.parts"
              ]);

$makcmd_java_classpath << "jar/parts.jar";
