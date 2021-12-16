#!/bin/bash

JARFILE=rebuild-boot.jar

echo "Use jar file [ $JARFILE ]"

PID=$(ps -ef | grep $JARFILE | grep -v grep | awk '{ print $2 }')
if [ -z "$PID" ]
then
  echo "Rebuild-Boot is already stopped"
else
  echo "Stopping Rebuild-Boot \(kill $PID\) ..."
  kill $PID
  sleep 10
fi

echo "Starting Rebuild-Boot ..."
nohup java -Xms1001M -Xmx1001M -XX:+UseG1GC -Djava.awt.headless=true -Drbpass= -DDataDirectory= -jar $JARFILE >/dev/null 2>&1 &
sleep 5

echo "done"
exit 0
