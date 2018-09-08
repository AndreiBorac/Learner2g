#!/bin/bash
# copyright (c) 2010 by andrei borac

SRCDIR="$1"
DSTDIR="$2"

set -o errexit
set -o nounset
set -o pipefail

if [ "$SRCDIR" == "" ]
then
  SRCDIR="cgen/zs42/mass"
fi

if [ "$DSTDIR" == "" ]
then
  DSTDIR="build/cgen/java/zs42/mass"
  mkdir -p "$DSTDIR"
fi

echo "SRCDIR='$SRCDIR'"
echo "DSTDIR='$DSTDIR'"

function filterA()
{
  sed -e 's/^package /\/* THIS FILE IS GENERATED; DO NOT EDIT *\/ package /'
}

###
# DX
###

for I in `seq 2 9`
do
  (
    echo '/* copyright (c) 2011 by andrei borac */'
    echo '/* THIS FILE IS GENERATED; DO NOT EDIT */'
    echo 'package zs42.mass;'
    echo -n 'public final class D'"$I"'<'
    for i in `seq 1 $I`
    do
      echo -n 'E'"$i"
      if [ "$i" != "$I" ]
      then
        echo -n ','
      fi
    done
    echo '>'
    echo ' extends RootObject {'
    for i in `seq 1 $I`
    do
      echo '  public final E'"$i"' d'"$i"';'
    done
    echo -n '  public D'"$I"'('
    for i in `seq 1 $I`
    do
      echo -n 'E'"$i"' d'"$i"
      if [ "$i" != "$I" ]
      then
        echo -n ', '
      fi
    done
    echo ')'
    echo '  {'
    for i in `seq 1 $I`
    do
      echo '    this.d'"$i"' = d'"$i"';'
    done
    echo '  }'
    echo '}'
  ) > "$DSTDIR"/D"$I".java
done

###
# FX
###

for I in `seq 0 9`
do
  (
    echo '/* copyright (c) 2011 by andrei borac */'
    echo '/* THIS FILE IS GENERATED; DO NOT EDIT */'
    echo 'package zs42.mass;'
    echo -n 'public abstract class F'"$I"'<'
    for i in `seq 0 $I`
    do
      echo -n 'E'"$i"
      if [ "$i" != "$I" ]
      then
        echo -n ','
      fi
    done
    echo -n '> extends RootObject { public abstract E0 invoke('
    for i in `seq 1 $I`
    do
      echo -n 'E'"$i"' e'"$i"
      if [ "$i" != "$I" ]
      then
        echo -n ','
      fi
    done
    echo '); }'
  ) > "$DSTDIR"/F"$I".java
done

###
# "STATIC" FILES
###

for NAME in
do
  cat < "$SRCDIR"/"$NAME".java |\
  filterA |\
  cat > "$DSTDIR"/"$NAME".java
done

###
# ARRAYS/BUFFERS
###

function filterC()
{
  sed -e 's/_N_/'"$1"'/g' |\
  sed -e 's/_E_/'"$2"'/g' |\
  sed -e 's/_Z_/'"$3"'/g'
}

function filterP() # primitive
{
  filterA |\
  filterC "$1" "$2" "$3" |\
  sed -e 's/_G_//g' |\
  sed -e 's/_O_//g' |\
  sed -e 's/new\[\[/new '"$2"'\[/g' |\
  sed -e 's/\]\]new/\]\[\]/g' |\
  sed -e 's/new\[/new '"$2"'\[/g' |\
  sed -e 's/\]new/\]/g' |\
  sed -e 's/_EPO_//g' |\
  sed -e 's/_LPO_//g' |\
  sed -e 's/_ELO_/\/*/g' |\
  sed -e 's/_LLO_/*\//g'
}

function filterZ() # boolean
{
  filterP "$1" "$2" "$3" |\
  sed -e 's/_EZO_//g' |\
  sed -e 's/_LZO_//g' |\
  sed -e 's/_EBO_/\/*/g' |\
  sed -e 's/_LBO_/*\//g' |\
  sed -e 's/_ENO_/\/*/g' |\
  sed -e 's/_LNO_/*\//g' |\
  sed -e 's/_EIO_/\/*/g' |\
  sed -e 's/_LIO_/*\//g' |\
  sed -e 's/_EFO_/\/*/g' |\
  sed -e 's/_LFO_/*\//g' |\
  sed -e 's/_DEFV_/'"$3"'/g' |\
  sed -e 's/_DECL_//g' |\
  sed -e 's/_TEST_//g' |\
  sed -e 's/_AEQB_/A == B/g' |\
  sed -e 's/_ALTB_/!A \& B/g' |\
  sed -e 's/_AGTB_/A \& !B/g'
}

function filterB() # byte
{
  filterP "$1" "$2" "$3" |\
  sed -e 's/_EZO_/\/*/g' |\
  sed -e 's/_LZO_/*\//g' |\
  sed -e 's/_EBO_//g' |\
  sed -e 's/_LBO_//g' |\
  sed -e 's/_ENO_/\/*/g' |\
  sed -e 's/_LNO_/*\//g' |\
  sed -e 's/_EIO_/\/*/g' |\
  sed -e 's/_LIO_/*\//g' |\
  sed -e 's/_EFO_/\/*/g' |\
  sed -e 's/_LFO_/*\//g' |\
  sed -e 's/_DEFV_/'"$3"'/g' |\
  sed -e 's/_DECL_//g' |\
  sed -e 's/_TEST_//g' |\
  sed -e 's/_AEQB_/A == B/g' |\
  sed -e 's/_ALTB_/A < B/g' |\
  sed -e 's/_AGTB_/A > B/g'
}

function filterN() # numeric (non-byte)
{
  filterP "$1" "$2" "$3" |\
  sed -e 's/_EZO_/\/*/g' |\
  sed -e 's/_LZO_/*\//g' |\
  sed -e 's/_EBO_/\/*/g' |\
  sed -e 's/_LBO_/*\//g' |\
  sed -e 's/_ENO_//g' |\
  sed -e 's/_LNO_//g' |\
  sed -e 's/_DEFV_/'"$3"'/g' |\
  sed -e 's/_DECL_//g' |\
  sed -e 's/_TEST_//g' |\
  sed -e 's/_AEQB_/A == B/g' |\
  sed -e 's/_ALTB_/A < B/g' |\
  sed -e 's/_AGTB_/A > B/g'

}

function filterI() # integral
{
  filterN "$1" "$2" "$3" |\
  sed -e 's/_EIO_//g' |\
  sed -e 's/_LIO_//g' |\
  sed -e 's/_EFO_/\/*/g' |\
  sed -e 's/_LFO_/*\//g'
}

function filterF() # floating-point
{
  filterN "$1" "$2" "$3" |\
  sed -e 's/_EIO_/\/*/g' |\
  sed -e 's/_LIO_/*\//g' |\
  sed -e 's/_EFO_//g' |\
  sed -e 's/_LFO_//g'
}

function filterL() # object
{
  filterA |\
  filterC "$1" "$2" "$3" |\
  sed -e 's/_G_/<E>/g' |\
  sed -e 's/_O_/<Object>/g' |\
  sed -e 's/new\[\[/aanewarray_unchecked(((E)(null)), (/g' |\
  sed -e 's/\]\]new/))/g' |\
  sed -e 's/new\[/anewarray_unchecked(((E)(null)), (/g' |\
  sed -e 's/\]new/))/g' |\
  sed -e 's/_EZO_/\/*/g' |\
  sed -e 's/_LZO_/*\//g' |\
  sed -e 's/_EBO_/\/*/g' |\
  sed -e 's/_LBO_/*\//g' |\
  sed -e 's/_ENO_/\/*/g' |\
  sed -e 's/_LNO_/*\//g' |\
  sed -e 's/_EIO_/\/*/g' |\
  sed -e 's/_LIO_/*\//g' |\
  sed -e 's/_EFO_/\/*/g' |\
  sed -e 's/_LFO_/*\//g' |\
  sed -e 's/_EPO_/\/*/g' |\
  sed -e 's/_LPO_/*\//g' |\
  sed -e 's/_ELO_//g' |\
  sed -e 's/_LLO_//g' |\
  sed -e 's/_DEFV_/'"$3"'/g' |\
  sed -e 's/_DECL_/int AcmpB/g' |\
  sed -e 's/_TEST_/AcmpB = proxy.compare(A, B)/g' |\
  sed -e 's/_AEQB_/AcmpB == 0/g' |\
  sed -e 's/_ALTB_/AcmpB < 0/g' |\
  sed -e 's/_AGTB_/AcmpB > 0/g'
}

function processPL()
{
  cat < "$SRCDIR"/"$2"X.java |\
  filter"$1" "$3" "$4" "$5" |\
  sed -s 's/_EXSZ_/'"$6"'/g' |\
  cat > "$DSTDIR"/"$2""$3".java
}

function processALL()
{
  processPL Z "$1" Z boolean '((boolean)(false))'  1
  processPL B "$1" B byte    '((byte)(0))'         1
  processPL I "$1" S short   '((short)(0))'        2
  processPL I "$1" I int     '((int)(0))'          4
  processPL I "$1" J long    '((long)(0))'         8
  processPL F "$1" F float   '((float)(0))'        4
  processPL F "$1" D double  '((double)(0))'       8
  processPL L "$1" L E       null  8 # assuming 64-bit
}

processALL s
processALL r
processALL c
processALL v
processALL w
#processALL m
processALL ri
processALL ci
processALL wi
