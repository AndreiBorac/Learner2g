#!/bin/false
# copyright (c) 2012 by andrei borac

makcmd_import("233-addc-gen-src",
              {
                "../233-addc/export" => /^ruby\/.*\.(rb)$/,
              });

makcmd_import("233-addc-src",
              {
                "../233-addc/export" => /^java\/.*\.(java)$/,
              });

makcmd_script("233-addc-gen", [ "233-addc-gen-src", ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p ./export",
               "mkdir -p ./java/zs42/addc",
               "ruby ./ruby/cgen.rb",
               "echo \"+OK (cgen.rb)\"",
              ]);

makcmd_script("233-addc-jar", [ "233-addc-src", "233-addc-gen", "057-parts-jar", "054-mass-jar", "086-buff-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-addc zs42.addc zs42.addc.test",
              ]);

$makcmd_java_classpath << "jar/zs42-addc.jar";

makcmd_script("233-addc-export", [ "233-addc-jar", "057-parts-jar", "054-mass-jar", "086-buff-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p export",
               "echo \"export CLASSPATH='\$CLASSPATH'\" > export/java_classpath",
               "cp -r jar export",
              ]);
