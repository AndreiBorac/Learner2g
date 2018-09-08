#!/bin/bash
# test.sh
# copyright (c) 2012 by andrei borac

set -o errexit
set -o nounset
set -o pipefail

IMPRB_IB=yes ./import.rb

taskset 0x1 java -Djava.net.preferIPv4Stack=true -Xmx48m -cp build/export/jar/complete.jar zs42.learner2g.Learner2g ORIGIN cooked COOKED_HOST httptest COOKED_PATH join.out SUPPORT_REWIND true ENFORCE_REWIND true ASSISTANT NONE ETCH_H 768 ETCH_W 1024 ETCH_UPS 30

echo "+OK"
