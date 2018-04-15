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

# Patch files need updating.
wait
exit 0

cp things/right.scad things/right-4x5.scad
cp things/left.scad things/left-4x5.scad
cp things/right-plate.scad things/right-4x5-plate.scad
$OPENSCAD -o things/right-4x5-plate.dxf things/right-4x5-plate.scad >/dev/null 2>&1 &
$OPENSCAD -o things/right-4x5.stl things/right-4x5.scad >/dev/null 2>&1 &
$OPENSCAD -o things/left-4x5.stl  things/left-4x5.scad >/dev/null 2>&1 &

patch -p1 < 4x6.patch
lein run src/dactyl_keyboard/dactyl.clj
cp things/right.scad things/right-4x6.scad
cp things/left.scad things/left-4x6.scad
cp things/right-plate.scad things/right-4x6-plate.scad
$OPENSCAD -o things/right-4x6-plate.dxf things/right-4x6-plate.scad >/dev/null 2>&1 &
$OPENSCAD -o things/right-4x6.stl things/right-4x6.scad >/dev/null 2>&1  &
$OPENSCAD -o things/left-4x6.stl  things/left-4x6.scad >/dev/null 2>&1 &
git checkout src/dactyl_keyboard/dactyl.clj

patch -p1 < 5x6.patch
lein run src/dactyl_keyboard/dactyl.clj
cp things/right.scad things/right-5x6.scad
cp things/left.scad things/left-5x6.scad
cp things/right-plate.scad things/right-5x6-plate.scad
$OPENSCAD -o things/right-5x6-plate.dxf things/right-5x6-plate.scad >/dev/null 2>&1 &
$OPENSCAD -o things/right-5x6.stl things/right-5x6.scad >/dev/null 2>&1  &
$OPENSCAD -o things/left-5x6.stl  things/left-5x6.scad >/dev/null 2>&1 &
git checkout src/dactyl_keyboard/dactyl.clj

patch -p1 < 6x6.patch
lein run src/dactyl_keyboard/dactyl.clj
cp things/right.scad things/right-6x6.scad
cp things/left.scad things/left-6x6.scad
cp things/right-plate.scad things/right-6x6-plate.scad
$OPENSCAD -o things/right-6x6-plate.dxf things/right-6x6-plate.scad >/dev/null 2>&1 &
$OPENSCAD -o things/right-6x6.stl things/right-6x6.scad >/dev/null 2>&1  &
$OPENSCAD -o things/left-6x6.stl  things/left-6x6.scad >/dev/null 2>&1 &
git checkout src/dactyl_keyboard/dactyl.clj


# git add things/*-4x5.stl
# git add things/right-4x5-plate.dxf
# git commit -m "Add CAD files"
wait
