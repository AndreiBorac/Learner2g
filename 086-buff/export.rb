#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("086-buff-src",
              {
                "../086-buff/export" => /.(rb|java)$/,
              });

makcmd_script("086-buff-gen", [ "086-buff-src" ],
              makcmd_prelude_bash() +
              [
               "mkdir -p java/zs42",
               "cp -r cgen/zs42/buff java/zs42",
               "find java/zs42/buff -type f | xargs ruby ruby/expando.rb"
              ]);

$makcmd_java_all_sources << "086-buff-gen";

makcmd_script("086-buff-jar", [ "086-buff-gen", "054-mass-jar" ],
              makcmd_prelude_bash_javac() +
              [
               #"find java/zs42/buff -type f | xargs cat",
               "javac_jar_packages buff zs42.buff"
              ]);

$makcmd_java_classpath << "jar/buff.jar";
