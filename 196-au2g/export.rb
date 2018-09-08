#!/bin/false
# copyright (c) 2012 by andrei borac

makcmd_import("196-au2g-gen-src",
              {
                "../196-au2g/export" => /\.(jnylus\.txt)$/,
              });

makcmd_script("196-au2g-gen", [ "196-au2g-gen-src", "193-ny4j-src", ],
              makcmd_prelude_bash() +
              [
               "mkdir -p java/zs42/au2g/ny4j/bind",
               "ruby ./ruby/jnylus.rb < cgen/ny4j/Audio2g.jnylus.txt",
               "mkdir -p export",
               "tar -f export/zs42-au2g-ny4j-bind.tar -C java/zs42/au2g/ny4j/bind -c .",
              ]);

$makcmd_java_all_sources << "196-au2g-gen";

makcmd_import("196-au2g-src",
              {
                "../196-au2g/export" => /\.(java)$/,
              });

$makcmd_java_all_sources << "196-au2g-src";

makcmd_script("196-au2g-jar", [ "196-au2g-src", "196-au2g-gen", "057-parts-jar", "054-mass-jar", "086-buff-jar", "193-ny4j-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-au2g zs42.au2g zs42.au2g.ny4j.bind",
              ]);

$makcmd_java_classpath << "jar/zs42-au2g.jar";

makcmd_script("196-au2g-export", [ "196-au2g-jar", "057-parts-jar", "054-mass-jar", "086-buff-jar", "193-ny4j-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p export",
               "echo \"export CLASSPATH='\$CLASSPATH'\" > export/java_classpath",
               "cp -r jar export",
              ]);
