PROJECT_VERSION := $(shell cat ./VERSION)
# You might need cygwin on Windows for this:
SAM ?= $(shell which sam 2>/dev/null || which sam.cmd 2>/dev/null)
MVN ?= mvn

compile: .version
	$(MVN) $(MVN_OPTIONS) compile

install: .version
	$(MVN) $(MVN_OPTIONS) install

deploy: .version
	$(MVN) $(MVN_OPTIONS) deploy

aws: .aws-sam

aws-deploy: aws
	 "$(SAM)" deploy $(SAM_DEPLOY_OPTIONS)

.version: VERSION
	$(MVN) $(MVN_OPTIONS) versions:set -DallowSnapshots=true -DnewVersion="$(PROJECT_VERSION)" || true
	$(MVN) $(MVN_OPTIONS) versions:commit
	echo "$(PROJECT_VERSION)" > .version

.aws-sam: .version
	"$(SAM)" build $(SAM_BUILD_OPTIONS)

clean:
	$(MVN) $(MVN_OPTIONS) clean
	rm -rf .aws-sam
	rm -f .version
