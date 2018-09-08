#!/bin/bash
# copyright (c) 2011 by andrei borac

ARG1="$1"

set -o errexit
set -o nounset
set -o pipefail

for i in blobs/*.png
do
  if [ -f "$i" ]
  then
    BASE="`basename $i .png`"
    
    if [ ! -f build/"$BASE".rgb ]
    then
      convert "$i" -depth 8 RGB:build/"$BASE".rgb
    fi
  fi
done

IMPRB_IB=yes ./import.rb

cd build/export
. java_classpath

java -Xmx256m -Xbatch -Dzs42.nats.codec.NaturalNumberCodec.sopath=so/libzs42natc.so zs42.pixels.codec.PixelsCodec'$'Test "$1" _003.ScreenCodec _002.BasicRleBitCodec _002.ScreenCodec.B _002.ScreenCodec.Z _002.WinnowRgbCodec _002.DifNatFilter _002.RleBitCodec _002.PlexNatCodec _002.XorBitFilter _001.RleBitCodec _001.PlainNatCodec _001.NoisyDifferentialNatCodec _001.PaletteNatCodec _001.NatDifRleCodec _001.RasterCodec _001.ScreenCodec

echo "+OK (test.sh)"
