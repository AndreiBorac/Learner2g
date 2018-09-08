#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("154-etch-src",
              {
                "../154-etch/export" => /.(java)$/
              });

$makcmd_java_all_sources << "154-etch-src";

makcmd_script("154-etch-jar", [ "154-etch-src", "054-mass-jar", "086-buff-jar", "057-parts-jar", "080-splitter-common-jar", "098-nats-codec-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages gotm-etch gotm.etch gotm.etch.teacher gotm.etch.test"
              ]);

$makcmd_java_classpath << "jar/gotm-etch.jar";

makcmd_script("154-etch-export", [ "154-etch-jar", "054-mass-jar", "086-buff-jar", "057-parts-jar", "080-splitter-common-jar", "098-nats-codec-jar", "192-proguard-container-tools", ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p export",
               "",
               "echo \"export CLASSPATH='\$CLASSPATH'\" > export/java_classpath",
               "",
               "mkdir ../temp/root",
               "",
               "for i in jar/*",
               "do",
               "  if [ -f \"\$i\" ]",
               "  then",
               "    ( cd ../temp/root ; fastjar -x -f ../../root/\"\$i\" )",
               "  fi",
               "done",
               "",
               "( cd ../temp/root ; fastjar -c -M -f ../complete.jar . )",
               "cp ../temp/complete.jar jar/",
               "",
               "cp -r jar export",
               "",
              ] + proguard_invoke([ [ "gotm-etch", "gotm.etch.teacher.EtchTeacher", { "main" => true, "repackageclasses" => "gotm.etch.teacher", "optimizations" => "!method/removal/parameter", } ] ]));
