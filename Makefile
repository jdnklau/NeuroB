# Enter project version here:
VER = 0.5

gradlebuild :
	./gradlew -q build

jars : clean
	@echo "***** Build .jar files"
	@./gradlew -q buildJars
	@echo "*****/ Done building jars"
	
clean :
	@echo "***** Clean gradle"
	@./gradlew -q clean
	@echo "*****/ Cleaned"

# how to run stuff
trainingset : distributedlibraryfile
	@echo "***** Beginning generation of training set"
	@echo "This will take a while. Maybe just come back tomorrow"
	java -jar build/libs/NeuroB-cli-$(VER).jar trainingset
	@echo "*****/ Training set generated"

distributedlibraryfile :
	@echo "***** Ensuring existence of LibraryIO.def in respective directories"
	java -jar build/libs/NeuroB-cli-$(VER).jar libraryIODef training_data/
	@echo "*****/ Libraries made"