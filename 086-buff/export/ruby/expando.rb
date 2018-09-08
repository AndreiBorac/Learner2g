#!/usr/bin/ruby
# copyright (c) 2011 by andrei borac

def process(inp)
  out = "";
  
  inp.split("// !!!").each_slice(3) { |head, spec, body|
    out << head;
    
    if (spec != nil)
      raise if (body == nil);
      spec = spec.split(" ");
      raise if (spec[0] != "for");
      nelm = spec[1].to_i;
      spec = spec[2..-1];
      patt = spec[0..nelm-1];
      spec[nelm..-1].each_slice(nelm) { |repl|
        frag = body;
        
        patt.zip(repl).each { |x, y|
          frag = frag.gsub(x, y);
        }
        
        out << frag;
      }
    end
  }
  
  return out;
end

ARGV.each { |filename|
  $stderr.puts("processing '" + filename + "'");
  content = IO.read(filename);
  File.open(filename, "w") { |f| f.write(process(content)); }
}

puts("+OK");
exit(0);
