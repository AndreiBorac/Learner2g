#!/bin/false
# copyright (c) 2011-2012 by andrei borac

makcmd_import("198-learner2g-stable-lantern-gen-src",
              {
                "../198-learner2g-stable/export" => /\.(jnylus\.txt)$/,
              });

makcmd_script("198-learner2g-stable-lantern-gen", [ "198-learner2g-stable-lantern-gen-src", "193-ny4j-src", ],
              makcmd_prelude_bash() +
              [
               "mkdir -p java/zs42/learner2g/lantern2g/ny4j/bind",
               "ruby ./ruby/jnylus.rb < cgen/ny4j/Lantern.jnylus.txt",
               "mkdir -p export",
               "tar -f export/zs42-learner2g-lantern2g-ny4j-bind.tar -C java/zs42/learner2g/lantern2g/ny4j/bind -c .",
              ]);

$makcmd_java_all_sources << "198-learner2g-stable-lantern-gen";

makcmd_import("198-learner2g-stable-src",
              {
                "../198-learner2g-stable/export" => /.(java)$/
              });

$makcmd_java_all_sources << "198-learner2g-stable-src";

makcmd_script("198-learner2g-stable-jar", [ "198-learner2g-stable-src", "198-learner2g-stable-lantern-gen", "057-parts-jar", "054-mass-jar", "086-buff-jar", "193-ny4j-jar", "098-nats-codec-jar", "080-splitter-common-jar", "196-au2g-jar", "233-addc-jar", "098-nats-codec-jar", "154-etch-jar", "148-nwed-jar", "094-gotm-onlf-stable-jar", ], # 094-gotm-onlf-stable-jar required for Teacher2g
              makcmd_prelude_bash_javac() +
              [
               "javac_jar_packages zs42-learner2g zs42.learner2g.groupir2g zs42.learner2g.station2g zs42.learner2g.deliver2g zs42.learner2g.control2g zs42.learner2g.teacher2g zs42.learner2g.student2g zs42.learner2g.lantern2g.ny4j.bind",
              ]);

$makcmd_java_classpath << "jar/zs42-learner2g.jar";

makcmd_script("198-learner2g-stable-export", [ "198-learner2g-stable-jar", "057-parts-jar", "054-mass-jar", "086-buff-jar", "193-ny4j-jar", "098-nats-codec-jar", "080-splitter-common-jar", "196-au2g-jar", "233-addc-jar", "154-etch-jar", "148-nwed-jar", "077-pixels-codec-jar", "094-gotm-onlf-stable-jar", "192-proguard-container-tools", ],
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
              ] + [
                   [ "zs42-learner2g-station2g", "zs42.learner2g.station2g", "Station2g", true  ],
                   [ "zs42-learner2g-deliver2g", "zs42.learner2g.deliver2g", "Deliver2g", true  ],
                   [ "zs42-learner2g-student2g", "zs42.learner2g.student2g", "Student2g", false ],
                   [ "zs42-learner2g-teacher2g", "zs42.learner2g.teacher2g", "Teacher2g", true  ],
                   [ "zs42-learner2g-control2g", "zs42.learner2g.control2g", "Control2g", true  ],
                  ].map{ |name, pckg, root, main| proguard_invoke([ [ name, (pckg + "." + root), { "main" => main, "repackageclasses" => pckg, "allowaccessmodification" => true, "optimizations" => "!method/removal/parameter", } ] ]); }.flatten);
