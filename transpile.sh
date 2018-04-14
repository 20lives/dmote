#!/bin/bash -eu
#: Transpile source changes to SCAD.
#: Optionally send SCAD files somewhere for rendering.

TARGET=${1:-}

while :
do
  echo "Transpiling."
  lein run _
  if [ -n "$TARGET" ]
  then
    echo "Transporting."
    rsync -a things/*.scad $TARGET &
  fi
  echo "Waiting for source changes."
  inotifywait -re CLOSE_WRITE src/
  sleep 1
done
