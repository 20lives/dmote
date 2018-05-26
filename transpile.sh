#!/bin/bash -eu
#: Transpile source changes to SCAD.
#: Optionally send SCAD files somewhere for rendering.

TARGET=${1:-}
RENDERER=${2:-}

while :
do
  echo "Transpiling."
  make $TARGET
  if [ -n "$RENDERER" ]
  then
    echo "Transporting."
    rsync -a things/*.scad $RENDERER &
  fi
  echo "Waiting for source changes."
  inotifywait -re CLOSE_WRITE src/
  sleep 1
done
