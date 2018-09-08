#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

mkdir -p build/classes
mkdir -p build/codegen/gotm/onlf/learner/common
javac -d build/classes java/tgen/gotm/onlf/learner/common/GenerateTables.java
java -cp build/classes tgen.gotm.onlf.learner.common.GenerateTables > build/codegen/gotm/onlf/learner/common/AudioCommonTables.java
javac -d build/classes build/codegen/gotm/onlf/learner/common/AudioCommonTables.java
