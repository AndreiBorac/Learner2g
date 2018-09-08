#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

java -cp build/classes -Djava.net.preferIPv4Stack=true gotm.onlf.splitter.client.Capture localhost 3771 pass_user 1 build/datafeed-"`date +%Yy%mm%dd%Hh%Mm%Ss%Nn`"

echo "+OK"
