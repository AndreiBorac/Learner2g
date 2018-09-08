#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

IMPRB_IB=yes ./import.rb

if [ -f ./prefs/HW ]
then
  . ./prefs/HW
else
  # defaults follow, DO NOT EDIT! instead, create a ./local/HW configuration file
  H=768
  W=1280
fi

# using httptest
java -Djava.net.preferIPv4Stack=true -Dgotm.onlf.utilities.Utilities.log=true -Xmx256m -cp build/export/complete.jar gotm.onlf.learner.student.GrandUnifiedInterconnect "$H" "$W" goodsofthemind.com 10012 d4ae778f9dc1a72fc0433206eade450130417348c7848d4d68418fa47d43eadd 1

echo "+OK"
