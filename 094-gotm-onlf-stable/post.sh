#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

IMPRB_IB=yes ./import.rb

if [ -f ./local/HW ]
then
  . ./local/HW
else
  # defaults follow, DO NOT EDIT! instead, create a ./local/HW configuration file
  H=768
  W=1280
fi

if [ -f ./local/gotm-onlf ]
then
  . ./local/gotm-onlf
else
  # defaults follow, DO NOT EDIT! instead, create a ./local/gotm-onlf configuration file
  UPS=50
fi

rm -rf --one-file-system /tmp/learner.in
mkdir -p /tmp/learner.in/www
mkdir -p /tmp/learner.in/build
cp build/export/*.jar /tmp/learner.in/www
cp build/export/*.jar /tmp/learner.in/build
cp -p pass_{root,user} /tmp/learner.in

echo hi > /tmp/learner.in/www/index.txt

tar -C /tmp/learner.in -c . | ssh root@httptest 'cat > /tmp/learner.tar'

ssh root@httptest 'cat > /tmp/post.sh' <<'EOF'
#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

if [ ! -f /tmp/.388fe2247b13c2b4c9fb4365507f8622 ]
then
  apt-get -y install thttpd
  touch /tmp/.388fe2247b13c2b4c9fb4365507f8622
fi

killall -w thttpd || true
killall -w java || true

rm -rf --one-file-system /tmp/learner
mkdir -p /tmp/learner
tar -C /tmp/learner -xf /tmp/learner.tar
cd /tmp/learner

if [ "$PROGUARD" == "y" ]
then
  SPLITTERJAR=build/pg-splitter.jar
  APPLETJAR=pg-applet.jar
else
  SPLITTERJAR=build/complete.jar
  APPLETJAR=complete.jar
fi

# while [ "`lsof -n | egrep '^socat.*LISTEN.$' | wc -l`" != "0" ]
# do
#   echo "... upper wait loop ..."
#   sleep 0.1
# done
# socat -d -d -d -d -D TCP4-LISTEN:9006,reuseaddr TCP4-LISTEN:9004 &> socat46.out &
# socat -d -d -d -d -D TCP4-LISTEN:9007,reuseaddr TCP4-LISTEN:9005 &> socat57.out &
# while [ "`lsof -n | egrep '^socat.*LISTEN.$' | wc -l`" != "2" ]
# do
#   echo "... lower wait loop ..."
#   sleep 0.1
# done

java -Djava.net.preferIPv4Stack=true -DUSE_128_BIT_VECTORS=true -Xmx256m -cp "$SPLITTERJAR" gotm.onlf.splitter.server.Splitter pass_root pass_user 9001 9002 9003 build/feedback-"`date +%Yy%mm%dd%Hh%Mm%Ss%Nn`" &> splitter.out &

cd /tmp/learner/www
cat > applet.html <<'EOF2'
<HTML>
<HEAD>
<TITLE>019-gotm-onlf</TITLE>
</HEAD>
<BODY>
<APPLET CODE="gotm.onlf.learner.student.StudentApplet" ARCHIVE="REPLACE_J" HEIGHT="400" WIDTH="400">
<PARAM NAME="DESKTOP_HEIGHT" VALUE="REPLACE_H"/>
<PARAM NAME="DESKTOP_WIDTH" VALUE="REPLACE_W"/>
<PARAM NAME="SPLITTER_HOST" VALUE="httptest"/>
<PARAM NAME="SPLITTER_PORT" VALUE="9002"/>
<PARAM NAME="SPLITTER_USER_PASS" VALUE="3f614161f39504f4143ce02010d36be0856609f0c7d90fea37371a0f2d1573ed"/>
<PARAM NAME="SPLITTER_WANT_BITS" VALUE="34"/>
<PARAM NAME="DESKTOP_UPDATES_PER_SEC" VALUE="REPLACE_UPS"/>
<PARAM NAME="USE_128_BIT_VECTORS" VALUE="note_this_enables_logging_to_stderr"/>
</APPLET>
</BODY>
</HTML>
EOF2

sed -e 's/REPLACE_J/'"$APPLETJAR"'/g' -e 's/REPLACE_H/'"$H"'/g' -e 's/REPLACE_W/'"$W"'/g' -e 's/REPLACE_UPS/'"$UPS"'/g' < applet.html > applet.html.tmp
mv applet.html.tmp applet.html

thttpd -r -u nobody -l logfile

echo "+OK"

EOF

PROGUARD="n"
if [ -f ./local/proguard ]
then
  PROGUARD="y"
fi

ssh root@httptest 'PROGUARD='"$PROGUARD"' H='"$H"' W='"$W"' UPS='"$UPS"' bash /tmp/post.sh </dev/null 2>&1'
