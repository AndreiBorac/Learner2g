#!/usr/bin/ruby
# pcktrcpt.rb - analyze pcktrcpt replies for common gaps
# copyright (c) 2012 by andrei borac

previous_source_tc = {};
previous_client_tc = {};

deltas = {};
minimum_deltas = {};
average_deltas = {};
standard_deviations = {};

IO.read("build/pcktrcpt/input").each_line { |line|
  sequence_number, header_pcktrcpt, client_id, header_stream_id, stream_id, header_source_tc, source_tc, header_client_tc, client_tc = line.split(" ");
  
  raise if (header_pcktrcpt  != "pcktrcpt");
  raise if (header_stream_id != "stream_id");
  raise if (header_source_tc != "source_tc");
  raise if (header_client_tc != "client_tc");
  
  sequence_number = sequence_number.to_i;
  
  source_tc = source_tc.to_i;
  client_tc = client_tc.to_i;
  
  previous_source_tc[client_id] = source_tc if (!(previous_source_tc[client_id]));
  previous_client_tc[client_id] = client_tc if (!(previous_client_tc[client_id]));
  
  delta_source_tc = ((source_tc - previous_source_tc[client_id]) & (2**32 - 1));
  delta_client_tc = ((client_tc - previous_client_tc[client_id]) & (2**32 - 1));
  
  previous_source_tc[client_id] = source_tc;
  previous_client_tc[client_id] = client_tc;
  
  delta = delta_client_tc - delta_source_tc;
  
  deltas[client_id] = [] if (!deltas[client_id]);
  deltas[client_id] << [ sequence_number, delta ];
};

#deltas.keys.to_a.each { |client_id|
#  deltas[client_id] = deltas[client_id][
#};

deltas.each { |client_id, points|
  minimum_deltas[client_id] = points.map{ |sequence_number, delta| delta; }.min;
  average_deltas[client_id] = ((points.map{ |sequence_number, delta| delta; }.inject(0){ |s, i| s + i; } + 0.0) / (points.length + 0.0));
  
  $stderr.puts("minimum_deltas[#{client_id}] = #{minimum_deltas[client_id]}");
  $stderr.puts("average_deltas[#{client_id}] = #{average_deltas[client_id]}");
};

deltas.keys.to_a.each { |client_id|
  minimum_delta = minimum_deltas[client_id];
  
  deltas[client_id] = deltas[client_id].map{ |sequence_number, delta| [ sequence_number, (delta - minimum_delta) ] };
  average_deltas[client_id] -= minimum_delta;
  
  minimum_deltas[client_id] = 0;
};

deltas.each { |client_id, points|
  average_delta = average_deltas[client_id];
  
  standard_deviation = 0;
  
  points.each { |sequence_number, delta|
    term = (delta - average_delta);
    term *= term;
    standard_deviation += term;
  };
  
  standard_deviation = Math.sqrt(standard_deviation);
  standard_deviations[client_id] = standard_deviation;
  
  $stderr.puts("standard_deviations[#{client_id}] = #{standard_deviations[client_id]}");
};

all_deltas = [];

deltas.each { |client_id, points|
  points.each { |sequence_number, delta| all_deltas << delta; };
};

badaud = [];

badaud.each { |client_id|
  deltas.delete(client_id);
};

shamt = 0;

deltas.keys.to_a.each { |client_id|
  minimum_delta = minimum_deltas[client_id];
  average_delta = average_deltas[client_id];
  deltas[client_id] = deltas[client_id].map{ |sequence_number, delta| [ (sequence_number + shamt), [ delta, 10000000 ].min ] };
  
  shamt += 1000;
};

deltas.each_pair { |client_id, points|
  File.open("build/pcktrcpt/#{client_id}", "w") { |f|
    f.puts(points.map{ |sequence_number, delta| "#{sequence_number} #{delta}" }.join("\n"));
  };
};

$stderr.puts("ctioga ...");
datalist = deltas.each_key.to_a.join(" ");
system("cd build/pcktrcpt; ctioga --no-auto-legend --line-width 0.1 #{datalist}");
