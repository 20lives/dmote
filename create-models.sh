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

lein run src/dactyl_keyboard/dactyl.clj
for F in things/*.scad
do
  $OPENSCAD -o ${F/scad/stl} $F >/dev/null 2>&1 &
done

wait
exit 0
