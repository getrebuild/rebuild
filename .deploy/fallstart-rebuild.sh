#!/bin/bash

RBHOME=/path/to/v-rebuild-standalone
cd $RBHOME

PID=$(ps -ef | grep $RBHOME | grep -v grep | awk '{ print $2 }')
if [ -z "$PID" ]
then
  echo "Rebuild-Standalone is not running. Starting ..."
  ./start-rebuild.sh
else
  echo "Rebuild-Standalone is running"
fi
