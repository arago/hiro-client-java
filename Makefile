#
# This Makefile does also work on Windows if you have Cygwin installed.
# WSL should work as well, although this is not tested.
#

PROJECT_VERSION := $(shell cat ./VERSION)
MVN ?= mvn

compile: .version
	$(MVN) $(MVN_OPTIONS) compile

install: .version
	$(MVN) $(MVN_OPTIONS) install

package: .version
	$(MVN) $(MVN_OPTIONS) package

deploy: .version
	$(MVN) $(MVN_OPTIONS) deploy

.version: VERSION
	$(MVN) $(MVN_OPTIONS) versions:set -DallowSnapshots=true -DnewVersion="$(PROJECT_VERSION)" || true
	$(MVN) $(MVN_OPTIONS) versions:commit
	echo "$(PROJECT_VERSION)" > .version

clean:
	$(MVN) $(MVN_OPTIONS) clean
	rm -rf .aws-sam
	rm -f .version
