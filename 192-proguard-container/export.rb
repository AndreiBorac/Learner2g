#!/bin/false
# copyright (c) 2011 by andrei borac

makcmd_import("192-proguard-container-tools",
              {
                "../192-proguard-container/remote" => /.(jar)$/
              });

def proguard_helper(injars, outjar, entry_class, options)
  out = "";
  
  out << "java -jar proguard.jar";
  out << " -injars " + injars + ".jar";
  out << " -outjars pg-" + outjar + ".jar";
  out << " -printmapping rt-" + outjar + ".map";
  out << " -libraryjars /usr/lib/jvm/default-java/jre/lib/rt.jar";
  out << " -keep public class " + entry_class;
  out << " ' { public static void main(java.lang.String[]); } '" if (options["main"]);
  out << " -keepclasseswithmembernames ' class * { native <methods>; } '";
  optimizations = options["optimizations"];
  out << " -optimizations '#{optimizations}'" if (optimizations != nil);
  out << " -optimizationpasses 3";
  out << " -overloadaggressively" if (options["overloadaggressively"]);
  repackageclasses = options["repackageclasses"];
  out << " -repackageclasses '#{repackageclasses}'" if (repackageclasses != nil);
  out << " -allowaccessmodification" if (options["allowaccessmodification"]);
  
  return out;
end

# exclude_all_optimizations: !class/marking/final,!class/merging/vertical,!class/merging/horizontal,!field/removal/writeonly,!field/marking/private,!field/propagation/value,!method/marking/private,!method/marking/static,!method/marking/final,!method/removal/parameter,!method/propagation/parameter,!method/propagation/returnvalue,!method/inlining/short,!method/inlining/unique,!method/inlining/tailrecursion,!code/merging,!code/simplification/variable,!code/simplification/arithmetic,!code/simplification/cast,!code/simplification/field,!code/simplification/branch,!code/simplification/string,!code/simplification/advanced,!code/removal/advanced,!code/removal/simple,!code/removal/variable,!code/removal/exception,!code/allocation/variable

# list = [ outjar:string, entry_class:string, options:string ]
def proguard_invoke(list)
  return [] if (!(File.exists?("./local/proguard")));
  
  loop1 = list.map{ |outjar, entry_class, options|
    proguard_helper("complete", outjar, entry_class, options);
  };
  
  loop2 = list.map{ |outjar, entry_class, options|
    [
     "echo -n \"pg-" + outjar + ".jar: \"",
     "fastjar -t -f \"pg-" + outjar + ".jar\" | wc -l",
    ]
  }.flatten;
  
  return\
  [
   "cp obtain/jar/proguard.jar ../temp",
   "rm -rf --one-file-system ../temp/pgrd",
   "mkdir -p ../temp/pgrd",
   "",
   "for i in jar/*",
   "do",
   "  if [ -f \"\$i\" ]",
   "  then",
   "    ( cd ../temp/pgrd ; fastjar -x -f ../../root/\"\$i\" )",
   "  fi",
   "done",
   "",
   "(",
   "  cd ../temp/pgrd",
   "  rm -rf --one-file-system META_INF/",
   "  fastjar -c -M -f ../complete.jar .",
   "  cd ..",
  ] + loop1 +\
  [
   "  ",
   "  echo -n \"complete.jar: \"",
   "  fastjar -t -f complete.jar | wc -l",
   "  ",
  ] + loop2 +\
  [
   ")",
   "",
   "rm ../temp/proguard.jar",
   "mkdir -p export/jar export/map",
   "mv ../temp/*.jar export/jar",
   "mv ../temp/*.map export/map",
  ];
  
  return lines;
end

def proguard_invoke_standard(list)
  return list.map{ |name, pckg, root, main| proguard_invoke([ [ name, (pckg + "." + root), { "main" => main, "repackageclasses" => pckg, "allowaccessmodification" => true, "optimizations" => "!method/removal/parameter", } ] ]); }.flatten;
end
