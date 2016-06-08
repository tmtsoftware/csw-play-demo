#!/bin/sh
#
# Creates a single install directory from all the csw stage directories.

dir=../install

hash sbt 2>/dev/null || { echo >&2 "Please install sbt first.  Aborting."; exit 1; }

test -d $dir || mkdir -p $dir/bin $dir/lib $dir/conf
sbt stage
for i in bin lib ; do cp -f */target/universal/stage/$i/* $dir/$i/; done
for i in bin lib conf ; do cp -f demo-web-server/target/universal/stage/$i/* $dir/$i/; done
rm -f $dir/bin/*.log.* $dir/bin/*.bat

