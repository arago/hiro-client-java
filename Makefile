VERSION := $(shell cat ./VERSION)

compile: set_version
	mvn $(MVN_OPTIONS) compile

install: set_version
	mvn $(MVN_OPTIONS) install

deploy: set_version
	mvn $(MVN_OPTIONS) deploy

set_version:
	mvn $(MVN_OPTIONS) versions:set -DallowSnapshots=true -DnewVersion="$(VERSION)" || true
	mvn $(MVN_OPTIONS) versions:commit

clean:
	mvn $(MVN_OPTIONS) clean