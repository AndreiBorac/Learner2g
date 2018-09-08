#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

java -cp build/classes -Djava.net.preferIPv4Stack=true gotm.onlf.splitter.client.Command localhost 3772 pass_root
echo "+OK"
