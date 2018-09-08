#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("080-splitter-common-src",
              {
                "../080-splitter-common/export" => /.(java)$/
              });

$makcmd_java_all_sources << "080-splitter-common-src";

makcmd_script("080-splitter-common-jar", [ "080-splitter-common-src", "054-mass-jar", "086-buff-jar" ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-splitter-common zs42.splitter.common"
              ]);

$makcmd_java_classpath << "jar/zs42-splitter-common.jar";
