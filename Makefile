

.PHONY: all
all: neurob

.PHONY: neurob
neurob:
	./gradlew core:assemble

.PHONY: clean
clean:
	./gradlew -q clean

.PHONY: test
test:
	./gradlew check

.PHONY: unit-test
unit-test:
	./gradlew test

.PHONY: it-test
it-test:
	./gradlew integrationTest
