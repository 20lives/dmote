#!/bin/bash -eu
# Prepare models for a git commit.

OPENSCAD=
if which openscad-nightly
then
  OPENSCAD=openscad-nightly
elif which openscad
then
  OPENSCAD=openscad
fi

[ -n "$OPENSCAD" ] || exit 65

make "$@"
for F in things/*.scad
do
  { $OPENSCAD -o ${F/scad/stl} $F >/dev/null 2>&1 && echo "Finished $F." ;} &
done

wait
exit 0
