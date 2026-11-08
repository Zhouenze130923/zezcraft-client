#!/bin/bash
set -e
cd /tmp/claude-0/zc
V=$1
J=/tmp/claude-0/dl/jdk-25.0.1+8/bin
sed -i "s/\"version\": *\"[^\"]*\"/\"version\": \"$V\"/" res/fabric.mod.json
rm -rf out && mkdir out
$J/javac -nowarn -proc:none --release 21 -cp "$(ls lib/*.jar | tr '\n' ':')" -d out $(find src -name '*.java') 2>&1 | grep -v "^Picked up" || true
test -f out/com/zezcraft/client/ui/R.class
cp -r res/* out/
rm -f zezclient-$V.jar
(cd out && $J/jar cf ../zezclient-$V.jar .) 2>&1 | grep -v "^Picked up" || true
ls -la zezclient-$V.jar
