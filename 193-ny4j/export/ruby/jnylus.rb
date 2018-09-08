#!/usr/bin/ruby
# jnylus.rb
# copyright (c) 2012 by andrei borac

class Array
  def map_with_index
    out = [];
    
    each_with_index{ |x, i|
      out << yield(x, i);
    };
    
    return out;
  end
  
  def join2(t, r, e)
    if (length > 0)
      return join(t + r) + t;
    else
      return e;
    end
  end
end

destination = nil;
package = nil;
imports = [];
access = "public";
invoke_prefix = "do";
handle_prefix = "on";
interfaces = [];
current = nil;

class NyMethod
  attr_accessor :ny_name;
  attr_accessor :ny_parameters;
  
  attr_accessor :parameter_block;
  attr_accessor :argument_block;
  
  def initialize(ny_name, ny_parameters)
    @ny_name = ny_name;
    @ny_parameters = ny_parameters;
  end
end

class NyInterface
  attr_accessor :ny_name;
  attr_accessor :ny_methods;
  
  def initialize(ny_name)
    @ny_name = ny_name;
    @ny_methods = [];
  end
end

STDIN.read().each_line{ |line|
  if (!(/^[ ]*#/.match(line)))
    words = line.gsub(" extends ", "_extends_").gsub(" super ", "_super_").split;
    
    if (words.length > 0)
      if (words[0] == "destination")
        raise if (!(words.length == 2));
        destination = words[1];
      end
      
      if (words[0] == "package")
        raise if (!(words.length == 2));
        package = words[1];
      end
      
      if (words[0] == "import")
        raise if (!(words.length == 2));
        imports << words[1];
      end
      
      if (words[0] == "access")
        raise if (!(words.length == 2));
        access = words[1];
      end
      
      if (words[0] == "prefixes")
        raise if (!(words.length == 3));
        invoke_prefix = words[1];
        handle_prefix = words[2];
      end
      
      if (words[0] == "interface")
        raise if (!(words.length == 2));
        current = NyInterface.new(words[1]);
        interfaces << current;
      end
      
      if (words[0] == "method")
        raise if (!(words.length >= 2));
        current.ny_methods << NyMethod.new(words[1], words[2..-1].map{ |i| i.gsub("_extends_", " extends ").gsub("_super_", " super "); });
      end
    end
  end
};

def default_value(type)
  t = type.strip;
  
  return "false"            if (t == "boolean");
  return "((" + t + ")(0))" if (/^(byte|short|char|int|long|float|double)$/.match(t));
  return "null";
end

interfaces.each{ |interface|
  serial = 0;
  
  body = [];
  
  if (package)
    body << "package #{package};";
    body << "";
  end
  
  if (imports.length > 0)
    imports.each{ |import|
      body << "import #{import};";
    };
    
    body << "";
  end
  
  ny = "zs42.ny4j.JNylus";
  
  body << <<EOF
#{access} abstract class #{interface.ny_name}
{
  private final #{ny}.Station station_caller;
  private final #{ny}.Station station_callee;
  private final #{ny}.Channel channel_caller;
  private final #{ny}.Channel channel_callee;
  
  protected #{interface.ny_name}(#{ny}.Station station_caller, #{ny}.Station station_callee, #{ny}.Channel channel_caller, #{ny}.Channel channel_callee)
  {
    this.station_caller = station_caller;
    this.station_callee = station_callee;
    this.channel_caller = channel_caller;
    this.channel_callee = channel_callee;
  }
  
  protected #{interface.ny_name}(#{ny}.Linkage linkage)
  {
    this(linkage.station_caller, linkage.station_callee, linkage.channel_caller, linkage.channel_callee);
  }
EOF
  
  interface.ny_methods.each{ |method|
    unique = (serial += 1);
    
    method.parameter_block = method.ny_parameters.map_with_index{ |p, i| "#{p} arg#{i}"; }.join(", ");
    method.argument_block = method.ny_parameters.map_with_index{ |p, i| "arg#{i}"; }.join(", ");
    
    body << <<EOF
  private static final class Container#{unique} implements Runnable
  {
    private final #{interface.ny_name} outer;
    private boolean refund;
    #{method.ny_parameters.map_with_index{ |p, i| "private #{p} arg#{i}"; }.join2(";", " ", ""); }
    
    Container#{unique}(#{interface.ny_name} outer) { this.outer = outer; }
    
    public void run()
    {
      if (refund) {
        outer.station_caller.assertOwnership();
        refund = false;
        outer.container_cache#{unique}.addFirst(this);
      } else {
        outer.station_callee.assertOwnership();
        outer.#{handle_prefix}#{method.ny_name}(#{method.argument_block});
        #{method.ny_parameters.map_with_index{ |p, i| "arg#{i} = #{default_value(p)}"; }.join2(";", " ", ""); }
        refund = true;
        outer.station_callee.post(outer.channel_caller, this);
      }
    }
  }
  
  private final zs42.parts.SimpleDeque<Container#{unique}> container_cache#{unique} = (new zs42.parts.SimpleDeque<Container#{unique}>());
  
  protected abstract void #{handle_prefix}#{method.ny_name}(#{method.parameter_block});
  
  #{access} final void #{invoke_prefix}#{method.ny_name}(#{method.parameter_block})
  {
    station_caller.assertOwnership();
    
    Container#{unique} container;
    
    if (container_cache#{unique}.isEmpty()) {
      container = (new Container#{unique}(this));
    } else {
      container = container_cache#{unique}.removeFirst();
    }
    
    #{method.ny_parameters.map_with_index{ |p, i| "container.arg#{i} = arg#{i}"; }.join2(";", " ", ""); }
    
    station_caller.post(channel_callee, container);
  }
EOF
  };
  
  body << <<EOF
  public static interface Handler
  {
EOF
  
  interface.ny_methods.each{ |method|
    body << <<EOF
    public void #{handle_prefix}#{method.ny_name}_#{interface.ny_name}(#{method.parameter_block});
EOF
  };
  
  body << <<EOF
  }
  
  public static #{interface.ny_name} wrapHandler(#{ny}.Station station_caller, #{ny}.Station station_callee, #{ny}.Channel channel_caller, #{ny}.Channel channel_callee, final Handler handler)
  {
    return
      (new #{interface.ny_name}(station_caller, station_callee, channel_caller, channel_callee)
        {
EOF
  
  interface.ny_methods.each{ |method|
    body << <<EOF
          protected void #{handle_prefix}#{method.ny_name}(#{method.parameter_block})
          {
            handler.#{handle_prefix}#{method.ny_name}_#{interface.ny_name}(#{method.argument_block});
          }
EOF
  };
  
  body << <<EOF
        });
  }
  
  public static #{interface.ny_name} wrapHandler(#{ny}.Linkage linkage, Handler handler)
  {
    return wrapHandler(linkage.station_caller, linkage.station_callee, linkage.channel_caller, linkage.channel_callee, handler);
  }
}
EOF
  
  File.new("#{destination}/#{interface.ny_name}.java", "wb").write(body.join2("\n", "\n", ""));
};
