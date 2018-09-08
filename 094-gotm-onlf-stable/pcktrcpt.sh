#!/bin/bash
# pcktrcpt.sh - analyze pcktrcpt replies for common gaps
# copyright (c) 2012 by andrei borac

set -o errexit
set -o nounset
set -o pipefail

mkdir -p build/pcktrcpt
if mountpoint -q build/pcktrcpt
then
  sudo umount build/pcktrcpt
fi
if mountpoint -q build/pcktrcpt
then
  exit 1
fi
sudo mount -t tmpfs none build/pcktrcpt

for i in local/pcktrcpt-source/*/feedback-[0-9]*
do
  echo "starting $i ..."
  java -cp build/export/pg-inspector.jar gotm.onlf.splitter.server.Inspector dump < "$i" | egrep '[0-9]+: pcktrcpt: [0-9]+: ' | tr '():,' '    ' > build/pcktrcpt/input
  ruby pcktrcpt.rb
  
  STAMP="`date +%s.%Ns`"
  
  cp build/pcktrcpt/Plot.pdf /tmp/pcktrcpt-plots/"$STAMP".pdf
  
  if [ -f build/pcktrcpt/Plot.png ]
  then
    cp build/pcktrcpt/Plot.png /tmp/pcktrcpt-plots/"$STAMP".png
  fi
done
