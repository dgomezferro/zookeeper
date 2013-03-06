#!/usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ZKS=bin/zkServer.sh
ZKSI=bin/zkServer-initialize.sh

if [ ! -d "conf" ]; then
    echo "run this from the toplevel directory"
    exit 1
fi

if [ ! `ls build/zookeeper*.jar` ]; then
    echo "first compile the zk jar file"
    exit 1
fi

DATADIR=test-scripts_datadir
DATALOGDIR=test-scripts_datalogdir

case "`uname`" in
    CYGWIN*) cygwin=true ;;
    *) cygwin=false ;;
esac

if $cygwin
then
    ZOOCFG=`cygpath -wp "$ZOOCFG"`
    # cygwin has a "kill" in the shell itself, gets confused
    KILL=/bin/kill
else
    KILL=kill
fi

fail() {
    # don't run clear_tmp to allow debugging
    echo "FAIL $1"
    $KILL -9 $(cat "$ZOOPIDFILE")
    $KILL -9 $$
}

#generate a minimal config
genconfig1() {
    cat > test-scripts.cfg <<EOF
tickTime=2000
initLimit=10
syncLimit=5
dataDir=$DATADIR
clientPort=19181
EOF
}

genconfig2() {
    genconfig1
    cat >> test-scripts.cfg <<EOF
dataLogDir=$DATALOGDIR
EOF
}

export ZOOCFGDIR=`pwd`
export ZOOCFG="test-scripts.cfg"
export CLASSPATH=$ZOOCFGDIR/conf

#clear out the clutter generated by scripts
clear_tmp() {
    rm -f test-scripts.cfg
    rm -fr $DATADIR
    rm -fr $DATALOGDIR
    rm -f zookeeper.out
}

start() {
    $CONFIG
    #ensure not already running
    $ZKS status && (echo "already running"; fail $LINENO)
    export ZOOPIDFILE="$DATADIR/zookeeper_server.pid"

    $ZKS start
}

stop() {
    $ZKS stop
}

CONFIG=genconfig1

clear_tmp
start
ls $DATADIR || fail $LINENO
ls $DATALOGDIR && fail $LINENO
stop

CONFIG=genconfig2

clear_tmp
start
ls $DATADIR || fail $LINENO

# zk actually checks for this to exist, but doesn't create
ls $DATALOGDIR && fail $LINENO

clear_tmp
mkdir -p "$DATALOGDIR"
start
ls $DATADIR || fail $LINENO
ls $DATALOGDIR || fail $LINENO
stop

#
# verify autocreate diabled
#
export ZOO_DATADIR_AUTOCREATE_DISABLE=1

CONFIG=genconfig1

clear_tmp
start
[ $? -eq 1 ] || fail $LINENO
ls $DATADIR && fail $LINENO
ls $DATALOGDIR && fail $LINENO

CONFIG=genconfig2

clear_tmp
mkdir -p "$DATADIR/version-2"
start
[ $? -eq 1 ] || fail $LINENO
ls $DATALOGDIR && fail $LINENO

CONFIG=genconfig1

clear_tmp
mkdir -p "$DATADIR/version-2"
start
[ $? -eq 0 ] || fail $LINENO
stop

CONFIG=genconfig2

clear_tmp
mkdir -p "$DATADIR/version-2"
mkdir -p "$DATALOGDIR/version-2"
start
[ $? -eq 0 ] || fail $LINENO
stop

#
# validate the initialize script
#

CONFIG=genconfig1

clear_tmp
$CONFIG

$ZKSI --configfile "$ZOOCFGDIR/$ZOOCFG"
ls $DATADIR || fail $LINENO

#ensure not already running
$ZKS status && (echo "already running"; fail $LINENO)
export ZOOPIDFILE="$DATADIR/zookeeper_server.pid"

$ZKS start
[ $? -eq 0 ] || fail $LINENO
stop


CONFIG=genconfig2

clear_tmp
$CONFIG

$ZKSI --configfile "$ZOOCFGDIR/$ZOOCFG"
ls $DATADIR || fail $LINENO
ls $DATALOGDIR || fail $LINENO

#ensure not already running
$ZKS status && (echo "already running"; fail $LINENO)
export ZOOPIDFILE="$DATADIR/zookeeper_server.pid"

$ZKS start
[ $? -eq 0 ] || fail $LINENO
stop


## validate force
CONFIG=genconfig1

clear_tmp
$CONFIG

$ZKSI --configfile "$ZOOCFGDIR/$ZOOCFG" || fail $LINENO
ls $DATADIR || fail $LINENO
$ZKSI --configfile "$ZOOCFGDIR/$ZOOCFG" && fail $LINENO
$ZKSI --force --configfile "$ZOOCFGDIR/$ZOOCFG" || fail $LINENO

#ensure not already running
$ZKS status && (echo "already running"; fail $LINENO)
export ZOOPIDFILE="$DATADIR/zookeeper_server.pid"

$ZKS start
[ $? -eq 0 ] || fail $LINENO
stop

$ZKSI --force --myid=1 --configfile "$ZOOCFGDIR/$ZOOCFG" || fail $LINENO


#done, cleanup and exit
clear_tmp
echo "SUCCESS"
