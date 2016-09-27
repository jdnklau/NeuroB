# Shortcut to run the Cli of NeuroB
RUNCLI = ./build/install/NeuroB/bin/NeuroB

all :
	@echo "***** Building gradle files"
	@./gradlew build
	@echo "*****/ Built"
	@echo "***** Create NeuroB binary"
	@./gradlew installDist
	@echo "*****/ Created binary"

jar : clean
	@echo "***** Build .jar file"
	@./gradlew CliFatJar
	@echo "*****/ Done building jar"
	
clean :
	@echo "***** Clean gradle"
	@./gradlew -q clean
	@echo "*****/ Cleaned"

# how to run stuff
trainingset : distributedlibraryfile
	@echo "***** Beginning generation of training set"
	@echo "This will take a while. Maybe just come back tomorrow"
	@$(RUNCLI) trainingset -dir prob_examples/public_examples/B/
	@echo "*****/ Training set generated"

distributedlibraryfile :
	@echo "***** Ensuring existence of LibraryIO.def in respective directories"
	@$(RUNCLI) libraryIODef -dir prob_examples/public_examples/B/
	@echo "*****/ Libraries made"