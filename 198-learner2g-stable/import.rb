#!/usr/bin/ruby
# copyright (c) 2011-2012 by andrei borac

$javac_jar_packages_additional_javac_flags = "-Xlint:-unchecked -Xlint:-rawtypes";

require("../053-build-scripts/ruby/import.rb");

require("../057-parts/export.rb");

require("../054-mass/export.rb");
require("../086-buff/export.rb");
require("../193-ny4j/export.rb");
require("../196-au2g/export.rb");
require("../233-addc/export.rb");

require("../080-splitter-common/export.rb");
require("../098-nats-codec/export.rb");

require("../148-nwed/export.rb");

require("../192-proguard-container/export.rb");

require("../154-etch/export.rb");

# enter kludge
require("../077-pixels-codec/export.rb");
require("../094-gotm-onlf-stable/export.rb");
# leave kludge

require("../198-learner2g-stable/export.rb");

makcmd_automatic_javadoc();
