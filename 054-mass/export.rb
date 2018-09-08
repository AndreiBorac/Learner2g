#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("054-mass-src",
              {
                "../054-mass/export" => /.(sh|java)$/,
              });

$makcmd_java_all_sources << "054-mass-src";

makcmd_script("054-mass-gen", [ "054-mass-src" ],
              makcmd_prelude_bash() +
              [
               "bash bash/codegen.sh cgen/zs42/mass java/zs42/mass"
              ]);

$makcmd_java_all_sources << "054-mass-gen";

makcmd_script("054-mass-jar", [ "054-mass-src", "054-mass-gen" ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages mass zs42.mass link.zs42.mass"
              ]);

$makcmd_java_classpath << "jar/mass.jar";
