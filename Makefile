

.PHONY: all
all: neurob

.PHONY: neurob
neurob: core cli

.PHONY: core
core:
	./gradlew core:assemble

.PHONY: cli
cli:
	./gradlew cli:installDist
	ln -sf ./cli/build/install/cli/bin/cli neurob-cli

.PHONY: clean
clean:
	./gradlew -q clean
	rm -r build/
	rm -r cli/out cli/bin
	rm -r core/out core/bin

.PHONY: test
test:
	./gradlew check

.PHONY: unit-test
unit-test:
	./gradlew test

.PHONY: it-test
it-test:
	./gradlew integrationTest
