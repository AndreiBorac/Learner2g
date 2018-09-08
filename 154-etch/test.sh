#!/bin/bash
# copyright (c) 2011 by andrei borac

set -o errexit
set -o nounset
set -o pipefail

IMPRB_IB=yes ./import.rb
mkdir -p build/testing
cd build/testing
[ -h ./so  ] || ln -s ../export/so  ./so
[ -h ./jar ] || ln -s ../export/jar ./jar
. ../export/java_classpath

#java -Xmx128m gotm.etch.Etch'$'DrawingCurvesTest 20 25 1234
java -Xmx128m gotm.etch.test.EtchTest'$'GenerateEventsTest

echo "+OK"
