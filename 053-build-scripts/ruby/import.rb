#!/bin/false
# copyright (c) 2011 by andrei borac

# to be sourced (via "require") from a project's import.rb
# (compilation script) before requiring each dependency project's
# export.rb

require("digest");
require("find");

###
# global trace flag
###

$global_trace_flag = true;

###
# timestamp
###

def timestamp()
  now = Time.now;
  return now.tv_sec.to_s + "." + now.tv_usec.to_s;
end

###
# directory stack
###

$dir_stack = [];

def dir_push(dir)
  $dir_stack.push(Dir.pwd());
  Dir.chdir(dir);
end

def dir_pull()
  Dir.chdir($dir_stack.pop());
end

###
# write file
###

def write_file(filename, lines)
  File.open(filename, "w") { |file| file.write(lines.join("\n") + "\n"); }
end

###
# run bash command line with error checking
###

$run_last_reported_pwd = "/invalid";

def run(cmd)
  if ($global_trace_flag)
    current_pwd = Dir.pwd;
    
    if (current_pwd != $run_last_reported_pwd)
      $stderr.puts("=====> ^cd " + current_pwd + "\$");
      $run_last_reported_pwd = current_pwd;
    end
    
    $stderr.puts("=====> ^" + cmd + "\$");
  end
  
  if (!system("bash", "-c", "set -o errexit; set -o nounset; set -o pipefail; " + cmd))
  then
    $stderr.puts("system invocation failed '" + cmd + "'");
    exit(1);
  end
end

###
# configure workspace environment
###

$workspace_directory_container = "/tmp/053-build-scripts";

run("mkdir -p " + $workspace_directory_container);

# clean up from previous runs

Dir.glob($workspace_directory_container + "/*").each { |workspace_directory_mountpoint|
  begin
    cleanup = false;
    
    if (File.exists?(workspace_directory_mountpoint + "/die"))
      cleanup = true;
    else
      pid = IO.read(workspace_directory_mountpoint + "/pid").strip.to_i;
      
      begin
        Process.getpgid(pid);
      rescue Errno::ESRCH
        cleanup = true;
      end
    end
    
    if (cleanup)
      # it's dead, so clean up (best-effort only; do not fail here)
      system("bash", "-c", "sudo umount '" + workspace_directory_mountpoint + "'");
      system("bash", "-c", "rmdir '" + workspace_directory_mountpoint + "'");
    end
  rescue Errno::ENOENT
    # nothing to do
  end
}

# initialize workspace directories

$workspace_directory_mountpoint = $workspace_directory_container + "/" + timestamp;

run("mkdir -p " + $workspace_directory_mountpoint);
run("sudo mount -t tmpfs tmpfs " + $workspace_directory_mountpoint);
run("sudo chown " + Process.uid.to_s + ":" + Process.gid.to_s + " " + $workspace_directory_mountpoint);
run("sudo chmod -t,go-rwx " + $workspace_directory_mountpoint);

#at_exit {
#  system("bash", "-c", "sudo umount '" + $workspace_directory_mountpoint + "'");
#  system("bash", "-c", "rm '" + $workspace_directory_mountpoint + "'");
#}

write_file($workspace_directory_mountpoint + "/pid", [ Process.pid.to_s ]);

$workspace_directory_temp = $workspace_directory_mountpoint + "/temp";
$workspace_directory_prev = $workspace_directory_mountpoint + "/prev";
$workspace_directory_save = $workspace_directory_mountpoint + "/save";
$workspace_directory_root = $workspace_directory_mountpoint + "/root";

###
# reset workspace directory
###

def workspace_directory_cleanup()
  run("rm --one-file-system -rf " + $workspace_directory_temp + " " + $workspace_directory_root);
  run("mkdir -p " + $workspace_directory_temp);
  run("mkdir -p " + $workspace_directory_prev);
  run("mkdir -p " + $workspace_directory_save);
  run("mkdir -p " + $workspace_directory_root);
end

workspace_directory_cleanup();

###
# configure build environment
###

run("
if [ ! -d ./build ]
then
  sudo mkdir ./build
  sudo chmod 0000 ./build
fi

if ! mountpoint -q ./build
then
  if mountpoint -q /tmp/build && [ -d /tmp/build/$(basename $(readlink -f .)) ]
  then
    sudo mount --bind /tmp/build/$(basename $(readlink -f .)) ./build
  else
    sudo mount -t tmpfs none ./build
  fi
fi
");

###
# internal utility functions
###

def makcmd_helper_calculate_descriptor(target, filall, commands)
  descriptor = [];
  
  filall.each { |dir, esa|
    esa.each { |rel|
      path = dir + "/" + rel;
      
      if (!File.exists?(path))
        raise("no such file '" + path + "', required by '" + target + "'");
      end
      
      mod = File.mtime(path);
      descriptor << path + " " + mod.tv_sec.to_s + "." + mod.tv_usec.to_s;
    }
  }
  
  return Digest::SHA256.hexdigest(([ "<files>" ] + descriptor.sort + [ "</files>", "<commands>" ] + commands + [ "</commands>"]).join("\n"));
end

def makcmd_helper_rebuild_required(target, descriptor_checksum)
  target_file = "build/" + target + ".makjar";
  target_desc = "build/" + target + ".makdes";
  
  if (!File.exists?(target_file) || !File.exists?(target_desc))
    # no target file (or no target descriptor file), so yes rebuild is required
    return true;
  else
    # rebuild required only if descriptor checksum has changed
    return descriptor_checksum != IO.read(target_desc).strip;
  end
end

def makcmd_helper_writeback_descriptor(target, descriptor_checksum)
  target_desc = "build/" + target + ".makdes";
  write_file(target_desc, [ descriptor_checksum ]);
end

def makcmd_helper_fastjar(output, filall)
  if ($global_trace_flag)
    $stderr.puts("makcmd_helper_fastjar(output='" + output.to_s + "', filall='" + filall.to_s + "')");
  end
  
  cmd = "fastjar -c -M -0 -f '" + output + "'";
  
  filall.each { |dir, esa|
    if (esa.length > 0)
      esa.each { |rel|
        cmd << " -C '" + dir + "' '" + rel + "'";
      }
    end
  }
  
  return cmd;
end

###
# public api
###

def makcmd_import(target, filmap)
  target_file = "build/" + target + ".makjar";
  
  # filmap is a mapping from directory names to regular expressions
  # filall is a mapping from directory names to a list of the matching files' relative paths
  
  filall = {};
  
  filmap.each { |dir, pat|
    listing = [];
    
    dir_push(dir);
    
    Find.find(".") { |filnam|
      if (File.file?(filnam))
        if (filnam.start_with?("./"))
          filnam = filnam[2..-1];
        end
        
        if (pat.match(filnam) != nil)
          listing << filnam;
        else
          # $stderr.puts("warning: makcmd_import: file '" + filnam + "' excluded by pattern '" + pat.to_s + "'");
        end
      end
    }
    
    dir_pull();
    
    filall[dir] = listing;
  }
  
  # calculate descriptor checksum
  
  descriptor_checksum = makcmd_helper_calculate_descriptor(target, filall, []);
  
  # now rebuild if it is required
  
  if (makcmd_helper_rebuild_required(target, descriptor_checksum))
    cmd = makcmd_helper_fastjar(target_file + ".tmp", filall) + "; mv '" + target_file + ".tmp' '" + target_file + "'";
    
    run(cmd);
    
    makcmd_helper_writeback_descriptor(target, descriptor_checksum);
  end
end

def makcmd_script_helper_save(target, build_directory)
  path = build_directory + "/" + target + ".savjar";
  
  dir_push($workspace_directory_save);
  run("fastjar -c -M -0 -f '" + path + "' *");
  dir_pull();
end

def makcmd_script_flagstr(target, sources, commands, flagstr)
  target_file = "build/" + target + ".makjar";
  
  # calculate descriptor checksum
  
  filall = { "build" => sources.map { |source| source + ".makjar" } };
  descriptor_checksum = makcmd_helper_calculate_descriptor(target, filall, commands);
  
  # now rebuild if it is required
  
  if (makcmd_helper_rebuild_required(target, descriptor_checksum))
    build_directory = Dir.pwd + "/build";
    
    # unpack source archives
    
    dir_push($workspace_directory_root);
    
    sources.each { |source|
      run("fastjar -x -f '" + build_directory + "/" + source + ".makjar'");
    }
    
    run("find . -type f > ../temp/before");
    
    dir_pull();
    
    # unpack previous ../save into ../prev if required and if it exists
    
    if (flagstr.include?("p"))
      path = build_directory + "/" + target + ".savjar";
      
      if (File.exists?(path))
        dir_push($workspace_directory_prev);
        run("fastjar -x -f '" + path + "'");
        dir_pull();
      end
    end
    
    # write command file
    
    dir_push($workspace_directory_temp);
    
    write_file("script", commands);
    run("chmod a+x script");
    
    dir_pull();
    
    # execute command file
    
    dir_push($workspace_directory_root);
    
    retv = system("bash", "-c", "set -o pipefail; env -i ../temp/script </dev/null 2>&1 | tee ../temp/output");
    
    # save ../save (only if always flag enabled or success indicator exists)
    unsaved = true;
    
    if (flagstr.include?("p"))
      $stderr.puts("flagstr.include?(\"a\")=#{flagstr.include?("a")}");
      $stderr.puts("flagstr.include?(\"c\")=#{flagstr.include?("c")}");
      $stderr.puts("File.exists?(\"../save/success\")=#{File.exists?("../save/success")}");
      
      if ((flagstr.include?("a") || (flagstr.include?("c") && File.exists?("../save/success"))))
        $stderr.puts("saving (early)");
        makcmd_script_helper_save(target, build_directory);
        unsaved = false;
      end
    end
    
    if (!retv)
    then
      system("bash", "-c", "cat ../temp/output");
      
      if (ENV["IMPRB_IB"])
      then
        # OLD BEHAVIOR: ignore all shell command trace lines
        #system("bash", "-c", "egrep --text -v '^[+]' < ../temp/output | less -b-1");
        
        # NEW BEHAVIOR: print the last shell command trace line and all subsequent lines
        #system("bash", "-c", "cat < ../temp/output | ( egrep --text -n '^[+]+[ ]' || true ) | tail -n 1 | cut -d: -f1 > ../temp/output_last_error_lineno");
        #system("bash", "-c", "cat < ../temp/output | tail -n +`cat ../temp/output_last_error_lineno` | less -b-1");
        
        # NEW BEHAVIOR: show all, start at end-of-file
        system("bash", "-c", "cat ../temp/output | less -b-1 +G");
        
        system("bash", "-c", "read -n 1 -r -s -t 0.5 X ; echo \"\$X\" > ../temp/gotten");
        
        if (IO.read("../temp/gotten").strip != "q")
        then
          dir_pull();
          write_file($workspace_directory_mountpoint + "/die", [ ]);
          exec("bash", "-c", "./import.rb");
        end
      end
      
      $stderr.puts("build script failed");
      exit(1);
    end
    
    run("cat ../temp/before | xargs -d '\\n' rm -f");
    run("fastjar -c -M -0 -f '" + build_directory + "/" + target + ".makjar.tmp' *");
    
    dir_pull();
    
    dir_push("build");
    
    run("mv '" + target + ".makjar.tmp' '" + target + ".makjar'");
    run("fastjar -x -f '" + target + ".makjar' export");
    
    dir_pull();
    
    # save ../save (only if not previously saved)
    if (flagstr.include?("p"))
      if (unsaved)
        $stderr.puts("saving (late)");
        makcmd_script_helper_save(target, build_directory);
      end
    end
    
    makcmd_helper_writeback_descriptor(target, descriptor_checksum);
    
    workspace_directory_cleanup();
  end
end

def makcmd_script(target, sources, commands)
  makcmd_script_flagstr(target, sources, commands, "");
end

###
# convenience definitions
###

# the following map ("glop", for "global options") is intended to hold
# all options that need to be passed between modules; it is not used
# by this script
$glop = {};

def glop_default(key, val)
  if (!$glop[key])
    $glop[key] = val;
  end
end

$makcmd_options = {};

def makcmd_prelude_bash()
  return \
  [
   "#!/bin/bash",
   "",
   "set -x",
   "",
   "set -o errexit",
   "set -o nounset",
   "set -o pipefail",
   "",
   "export PATH='/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin'",
  ];
end

$makcmd_java_all_sources = [];
$makcmd_java_classpath = [];

def makcmd_prelude_bash_javac()
  return makcmd_prelude_bash() +
    [
     "export CLASSPATH='" + $makcmd_java_classpath.join(":") + "'",
     "",
     "function javac_jar_packages_helper_xlat_java()",
     "{",
     "  for i in \$1",
     "  do",
     "    echo java/\${i//.//}",
     "  done",
     "}",
     "",
     "function javac_jar_packages_helper_xlat_class()",
     "{",
     "  for i in \$1",
     "  do",
     "    echo \${i//.//}",
     "  done",
     "}",
     "",
     "function javac_jar_packages()",
     "{",
     "  JAR=\"\$1\"",
     "  shift",
     "  PACKAGES=\"\$*\"",
     "  ",
     "  mkdir -p jar",
     "  ",
     "  STAMP=\"`date +%s.%N`\"",
     "  mkdir -p ../temp/\"\$STAMP\"/classes",
     "  ",
     "  find \$(javac_jar_packages_helper_xlat_java \"\$PACKAGES\") -type f > ../temp/\"\$STAMP\"/.java_files",
     "  ( set +o errexit; javac -Xlint -Xlint:-path -Xlint:-serial -Xlint:-cast #{$javac_jar_packages_additional_javac_flags} -d ../temp/\"\$STAMP\"/classes @../temp/\"\$STAMP\"/.java_files 2>&1 | egrep -v 'uses unchecked or unsafe operations.' | egrep -v 'Recompile with -Xlint:unchecked for details.' | tee ../temp/\"\$STAMP\"/.java_output ; true )",
     "  ",
     "  if [ -s ../temp/\"\$STAMP\"/.java_output ]",
     "  then",
     "    exit 1",
     "  fi",
     "  ",
     "  (",
     "    cd ../temp/\"\$STAMP\"/classes",
     "    fastjar -c -f ../../../root/jar/\"\$JAR\".jar \$(javac_jar_packages_helper_xlat_class \"\$PACKAGES\")",
     "  )",
     "}",
     ""
  ];
end

def makcmd_prelude_bash_javadoc_everything()
  return makcmd_prelude_bash() +
    [
     "mkdir -p export/javadoc",
     "",
     "STAMP=\"`date +%s.%N`\"",
     "mkdir -p ../temp/\"\$STAMP\"/classes",
     "",
     "find java -type f > ../temp/\"$STAMP\"/.java_files",
     "javadoc -d export/javadoc -private @../temp/\"$STAMP\"/.java_files"
    ];
end

def makcmd_launch_javadoc()
  system("bash", "-c", "my-launch-firefox.sh -copy `readlink -f build/export/javadoc` +copy -rel javadoc/index.html");
end

if (ARGV.include?("see"))
  makcmd_launch_javadoc();
  exit(0);
end

def makcmd_automatic_javadoc()
  interval_file = "build/automatic-javadoc-interval";
  
  want_javadoc = ARGV.include?("doc");
  auto_javadoc = false;
  
  if (File.exists?(interval_file))
    auto_javadoc = (Time.now.tv_sec > (IO.read(interval_file).strip.to_i + (1 * 60 * 60)));
  else
    auto_javadoc = true;
  end
  
  if (want_javadoc || auto_javadoc)
    makcmd_script("053-automatic-javadoc", $makcmd_java_all_sources, makcmd_prelude_bash_javadoc_everything());
    write_file(interval_file, [ Time.now.tv_sec.to_s ]);
  end
  
  if (want_javadoc)
    makcmd_launch_javadoc();
  end
end
