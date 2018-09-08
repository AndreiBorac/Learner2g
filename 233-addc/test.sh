#!/bin/bash
# test.sh
# copyright (c) 2012 by andrei borac

set -o errexit
set -o nounset
set -o pipefail

set -x

IMPRB_IB=yes ./import.rb

if [ ./local/data/atr-khz48.wav -nt ./build/atr-khz48.wav ]
then
  cat <./local/data/atr-khz48.wav >./build/atr-khz48.wav
fi

if [ ./local/data/utc1356030940979ms -nt ./build/mic.raw ]
then
  cat <./local/data/utc1356030940979ms >./build/mic.raw
fi

cd build

CMD="java -Xms64m -Xmx64m -cp ./export/jar/zs42-addc.jar zs42.addc.test.AudioCodecCLI"

NOR="-b 16 -B -c 1 -e signed-integer"
FMT="$NOR"" -r 8k"

sox $NOR -r 48k ./mic.raw ./mic.wav

for sampX in samp1
do
  $CMD "$sampX" ./mic.raw ./mic-"$sampX".raw $(( (48/8) ))
  sox $FMT ./mic-"$sampX".raw ./mic-"$sampX".wav
  
  $CMD enlaw ./mic-"$sampX".raw ./mic-"$sampX"-enlaw.raw
  
  for XXlaw in unlaw melaw delaw
  do
    $CMD "$XXlaw" ./mic-"$sampX"-enlaw.raw ./mic-"$sampX"-enlaw-"$XXlaw".raw
    sox $FMT ./mic-"$sampX"-enlaw-"$XXlaw".raw ./mic-"$sampX"-enlaw-"$XXlaw".wav
  done
done

exit 0

for i in samp1 samp2
do
  $CMD "$i" ./mic.raw ./mic-"$i".raw $(( (48/8) ))
  sox $FMT ./mic-"$i".raw ./mic-"$i".wav
done

exit 0

# old

for HZ in 8 12 16
do
  NOR="-b 16 -B -c 1 -e signed-integer"
  FMT="$NOR"" -r ""$HZ""k"
  
  sox ./atr-khz48.wav $NOR ./atr-khz48.raw
  
  sox ./atr-khz48.wav $FMT ./atr-khz"$HZ".raw
  sox $FMT ./atr-khz"$HZ".raw ./atr-khz"$HZ".wav
  
  $CMD samp1 ./atr-khz48.raw ./atr-khz"$HZ"spl.raw $(( (48/HZ) ))
  sox $FMT ./atr-khz"$HZ"spl.raw ./atr-khz"$HZ"spl.wav
  
  $CMD enlaw ./atr-khz"$HZ".raw ./atr-khz"$HZ"law.raw
  
  $CMD unlaw ./atr-khz"$HZ"law.raw ./atr-khz"$HZ"ulw.raw
  sox $FMT ./atr-khz"$HZ"ulw.raw ./atr-khz"$HZ"ulw.wav
  
  $CMD melaw ./atr-khz"$HZ"law.raw ./atr-khz"$HZ"mlw.raw
  sox $FMT ./atr-khz"$HZ"mlw.raw ./atr-khz"$HZ"mlw.wav
  
  $CMD delaw ./atr-khz"$HZ"law.raw ./atr-khz"$HZ"dlw.raw
  sox $FMT ./atr-khz"$HZ"dlw.raw ./atr-khz"$HZ"dlw.wav
done
