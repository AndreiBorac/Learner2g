#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

#java -cp build/classes -Djava.net.preferIPv4Stack=true gotm.onlf.splitter.server.Splitter pass_root pass_user 3770 3771 3772 build/feedback-"`date +%Yy%mm%dd%Hh%Mm%Ss%Nn`"

# using httptest
java -Xmx512m -Djava.net.preferIPv4Stack=true -cp build/compound.jar gotm.onlf.splitter.server.Splitter pass_root pass_user 9001 9002 9003 build/feedback-"`date +%Yy%mm%dd%Hh%Mm%Ss%Nn`"

echo "+OK"
