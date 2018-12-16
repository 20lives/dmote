# GNU makefile. This compiles to Java at need and combines bundled
# YAML configuration files into demonstration models of the DMOTE.
# https://www.gnu.org/software/make/manual/make.html

.PHONY: default visualization orthographic flat threaded threaded-visualization solid all docs test clean

OBJECTS = $(shell find src/)

default: target/dmote.jar
	java -jar target/dmote.jar

visualization: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/visualization.yaml

orthographic: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/orthographic_layout.yaml

flat: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/flat_layout.yaml

threaded-mutual: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/wrist/threaded_mutual.yaml

threaded-caseside: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/wrist/threaded_caseside.yaml

threaded-visualization: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/wrist/threaded_caseside.yaml -c resources/opt/visualization.yaml

doc/options-main.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters main > doc/options-main.md

doc/options-clusters.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters clusters > doc/options-clusters.md

doc/options-nested.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters nested > doc/options-nested.md

doc/options-wrist-rest-mounts.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters wrist-rest-mounts > doc/options-wrist-rest-mounts.md

target/dmote.jar: $(OBJECTS)
	lein uberjar

docs: doc/options-main.md doc/options-clusters.md doc/options-nested.md doc/options-wrist-rest-mounts.md

test:
	lein test

# “all” will overwrite its own outputs.
# Intended for code sanity checking before pushing a commit.
all: test docs default threaded-visualization orthographic flat threaded solid

clean:
	-rm things/scad/*.scad && rmdir things/scad/
	-rm things/stl/*.stl && rmdir things/stl/
	lein clean
