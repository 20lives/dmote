# GNU makefile. https://www.gnu.org/software/make/manual/make.html

# By default, CONFDIR is the bundled configuration file directory.
CONFDIR ?= config/

# Real prior artefacts are charted as this makefile is parsed.
YAML := $(shell find $(CONFDIR) -name '*.yaml')
SOURCECODE := $(shell find src -type f)

# YAML files are not made from here but are treated as targets anyway.
# This is a means of activating them by naming them as CLI arguments.
.PHONY: $(YAML) dmote_62key vis mutual caseside all docs test clean

# CONFFILES is a space-separated array of relative paths to selected
# YAML files, starting with a near-neutral base.
CONFFILES := $(CONFDIR)base.yaml

# The append_config function is what adds (more) YAML filepaths to CONFFILES.
# If not already present in the path, CONFDIR will be prepended to each path.
# This will break if CONFDIR is duplicated in the argument.
define append_config
	$(eval CONFFILES += $(CONFDIR)$(subst $(CONFDIR),,$$1))
endef

# Targets and their recipes follow.

# The %.yaml pattern target ensures that each YAML file named as a target,
# including each prerequisite named below, is appended to CONFFILES.
%.yaml:
	$(call append_config,$@)

# The dmote_62key target, acting as the default, builds SCAD files.
# When resolved, its recipe constructs a Java command where each
# selected configuration file gets its own -c parameter.
dmote_62key: target/dmote.jar dmote/base.yaml
	java -jar target/dmote.jar $(foreach FILE,$(CONFFILES),-c $(FILE))

# Curated shorthand for configuration fragments. These run no shell commands.
vis: visualization.yaml
mutual: dmote/wrist/threaded_mutual.yaml
caseside: dmote/wrist/threaded_caseside.yaml

# The remainder of this file describes more typical Make work, starting with
# the compilation of the Clojure application into a Java .jar and specific
# pieces of documentation.

target/dmote.jar: $(SOURCECODE)
	lein uberjar

doc/options-main.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters main > doc/options-main.md

doc/options-clusters.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters clusters > doc/options-clusters.md

doc/options-nested.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters nested > doc/options-nested.md

doc/options-wrist-rest-mounts.md: target/dmote.jar
	java -jar target/dmote.jar --describe-parameters wrist-rest-mounts > doc/options-wrist-rest-mounts.md

docs: doc/options-main.md doc/options-clusters.md doc/options-nested.md doc/options-wrist-rest-mounts.md

test:
	lein test

# The “all” target is intended for code sanity checking before pushing a commit.
all: test docs visualization mutual dmote_62key

clean:
	-rm things/scad/*.scad
	-rmdir things/scad/
	-rm things/stl/*.stl
	-rmdir things/stl/
	lein clean
