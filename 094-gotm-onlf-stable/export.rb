#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("094-gotm-onlf-stable-tab",
              {
                "../094-gotm-onlf-stable/export" => /^java\/tgen\/gotm\/onlf\/learner\/common\/GenerateTables.java$/
              });

makcmd_script("094-gotm-onlf-stable-gen", [ "094-gotm-onlf-stable-tab" ],
              makcmd_prelude_bash() +
              [
               "mkdir -p ../temp/classes",
               "javac -d ../temp/classes java/tgen/gotm/onlf/learner/common/GenerateTables.java",
               "mkdir -p                                                                     java/gotm/onlf/learner/common",
               "java -cp ../temp/classes      tgen.gotm.onlf.learner.common.GenerateTables > java/gotm/onlf/learner/common/AudioCommonTables.java"
              ]);

$makcmd_java_all_sources << "094-gotm-onlf-stable-gen";

makcmd_import("094-gotm-onlf-stable-src",
              {
                "../094-gotm-onlf-stable/export" => /.(java)$/
              });

$makcmd_java_all_sources << "094-gotm-onlf-stable-src";

makcmd_script("094-gotm-onlf-stable-jar", [ "094-gotm-onlf-stable-src", "094-gotm-onlf-stable-gen", "054-mass-jar", "086-buff-jar", "057-parts-jar", "077-pixels-codec-jar", "098-nats-codec-jar", "148-nwed-jar", "154-etch-jar", "080-splitter-common-jar", ],
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages gotm-onlf zs42.junction gotm.onlf.utilities gotm.onlf.splitter.common gotm.onlf.splitter.server gotm.onlf.splitter.client gotm.onlf.learner.common gotm.onlf.learner.control gotm.onlf.learner.student gotm.onlf.learner.teacher"
              ]);

$makcmd_java_classpath << "jar/gotm-onlf.jar"

makcmd_script("094-gotm-onlf-stable-export", [ "054-mass-jar", "086-buff-jar", "057-parts-jar", "077-pixels-codec-jar", "098-nats-codec-jar", "148-nwed-jar", "154-etch-jar", "080-splitter-common-jar", "094-gotm-onlf-stable-jar", "192-proguard-container-tools", ],
              makcmd_prelude_bash_javac() +
              [
               "mkdir -p export",
               "",
               "echo \"export CLASSPATH='\$CLASSPATH'\" > export/java_classpath",
               "",
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
              ] + [
                   [ "splitter",    "gotm.onlf.splitter.server", "Splitter", true ],
                   [ "interleaver", "gotm.onlf.splitter.server", "Interleaver", true ],
                   [ "inspector",   "gotm.onlf.splitter.server", "Inspector", true ],
                   [ "capture",     "gotm.onlf.splitter.client", "Capture", true ],
                   [ "admin",       "gotm.onlf.learner.control", "Admin", true ],
                   [ "applet",      "gotm.onlf.learner.student", "StudentApplet", false ],
                   [ "cli",         "gotm.onlf.learner.teacher", "CommandLineInterface", true ]
                  ].map{ |name, pckg, root, main| proguard_invoke([ [ name, (pckg + "." + root), { "main" => main, "repackageclasses" => pckg, "allowaccessmodification" => true, "optimizations" => "!method/removal/parameter", } ] ]); }.flatten);
