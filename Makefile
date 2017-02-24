# Shortcut to run the Cli of NeuroB
RUNCLI = ./build/install/NeuroB/bin/NeuroB
EXAMPLES = examples/prob_examples/public_examples

all :
	@echo "***** Building gradle files"
	@./gradlew build
	@echo "*****/ Built"

install :
	@echo "***** Create NeuroB binary"
	@./gradlew installDist
	@echo "*****/ Created binary"

jar :
	@echo "***** Build .jar file"
	@./gradlew CliFatJar
	@echo "*****/ Done building jar"

clean :
	@echo "***** Clean gradle"
	@./gradlew -q clean
	@echo "*****/ Cleaned"

dev :
	@echo "***** Setting up eclipse project"
	@./gradlew cleanEclipse
	@./gradlew eclipse
	@echo "*****/ Eclipse project set up"

# how to run stuff
trainingset : distributedlibraryfile
	@echo "***** Beginning generation of training set"
	@echo "This will take a while. Maybe just come back tomorrow"
	@$(RUNCLI) trainingset -dir $(EXAMPLES)
	@echo "*****/ Training set generated"

alltrainingsets : distributedlibraryfile
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf solclass -solver prob
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf solclass -solver kodkod
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf solsel
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf soltime
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi solclass -solver prob -size 32
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi solclass -solver kodkod -size 32
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi solsel -size 32
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi soltime -size 32

distributedlibraryfile :
	@echo "***** Ensuring existence of LibraryIO.def in respective directories"
	@$(RUNCLI) libraryIODef -dir examples/prob_examples/public_examples/B/
	@echo "*****/ Libraries made"
