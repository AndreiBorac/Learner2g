#!/bin/bash
# copyright (c) 2011 by andrei borac

set -o errexit
set -o nounset
set -o pipefail

IMPRB_IB=yes ./import.rb

STAMP="`date +%Yy%mm%dd-%Hh%Mm%Ss-%Nn`"

rm -f build/log
ln -s log-"$STAMP" build/log

(
  . ../079-killall-matching/export.sh
  
  function stop()
  {
    killall_matching 'java -Dmarker=gotm.onlf.learner.teacher.CommandLineInterface'
    (
      cd ../097-pixels-teacher
      ./stop.sh
    )
    killall_matching 'appletviewer'
  }
  
  stop
  
  ./post.sh
  ./cli.sh </dev/null 2>&1 | sed -u -e 's/^/audio : /' &
  (
    cd ../097-pixels-teacher
    ./test.sh </dev/null 2>&1 | sed -u -e 's/^/screen: /' &
  )
  
  taskset 0x2 appletviewer -J-Djava.net.preferIPv4Stack=true -J-Xmx512m http://httptest/applet.html </dev/null 2>&1 | sed -e 's/^/applet1: /' &
  
  read -n 1 IGNORED
  
  stop
  
  sleep 1
) 2>&1 | tee build/log-"$STAMP"
