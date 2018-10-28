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

threaded: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/threaded_wrist_rest.yaml

threaded-visualization: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/threaded_wrist_rest.yaml -c resources/opt/visualization.yaml

solid: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/solid_wrist_rest.yaml

solid-visualization: target/dmote.jar
	java -jar target/dmote.jar -c resources/opt/solid_wrist_rest.yaml -c resources/opt/visualization.yaml

doc/options-main.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters main > doc/options-main.md

doc/options-clusters.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters clusters > doc/options-clusters.md

doc/options-nested.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters nested > doc/options-nested.md

docs: doc/options-main.md doc/options-clusters.md doc/options-nested.md

target/dmote.jar: $(OBJECTS)
	lein uberjar

test:
	lein test

# “all” will overwrite its own outputs.
# Intended for code sanity checking before pushing a commit.
all: test docs default threaded-visualization orthographic flat threaded solid

clean:
	-rm things/*.scad
	lein clean
