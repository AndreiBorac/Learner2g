#!/bin/bash

mkdir -p build/classes
while [ 1 ]
do
  if [ ! -d build/codegen ]
  then
    ./task-tgen.sh
  fi
  javac -Xlint:unchecked -d build/classes `git ls-files java | egrep '\.java$'` `find build/codegen | egrep '\.java$'` &> build/log
  RETV="$?"
  if [ "$RETV" != "0" ]
  then
    clear
    less -S build/log
    continue
  else
    cat build/log
    break
  fi
done

if [ "$1" == "jar" ]
then
  (
    mkdir -p build/applet
    cd build/classes
    jar cf ../applet/StudentApplet.jar zs42 gotm
    cd ../applet
    cat > applet.html << 'EOF'
<HTML>
<BODY>
<APPLET ARCHIVE="StudentApplet.jar" CODE="gotm.onlf.learner.student.StudentApplet" HEIGHT="600" WIDTH="620">
<PARAM NAME="SPLITTER_HOST" VALUE="httptest"/>
<PARAM NAME="SPLITTER_PORT" VALUE="9002"/>
<PARAM NAME="SPLITTER_USER_PASS" VALUE="3f614161f39504f4143ce02010d36be0856609f0c7d90fea37371a0f2d1573ed"/>
<PARAM NAME="SPLITTER_WANT_BITS" VALUE="1"/>
</APPLET>
</BODY></HTML>
EOF
#    google-chrome file://`readlink -f applet.html`
  )
fi

if [ "$1" == "doc" ]
then
  rm -rf build/javadoc
  javadoc -d build/javadoc -classpath build -private `git ls-files java | egrep '\.java$'`
  my-launch-chrome.sh file://`readlink -f build/javadoc/index.html` 2>&1 >/dev/null
fi
