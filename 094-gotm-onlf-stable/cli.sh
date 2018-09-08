#!/bin/bash

# Assumes that the server is already running

set -o errexit
set -o nounset
set -o pipefail

if [ ! -f ./local/TargetDataLine ]
then
  echo "missing ./local/TargetDataLine"
  exit 1
fi

# using httptest
java -Dmarker=gotm.onlf.learner.teacher.CommandLineInterface -Djava.net.preferIPv4Stack=true -DUSE_128_BIT_VECTORS=true -Xmx512m -cp build/export/complete.jar gotm.onlf.learner.teacher.CommandLineInterface "`cat ./local/TargetDataLine`" "default" "httptest" 9001 1 pass_root 2048
echo "+OK"
