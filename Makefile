# Enter project version here:
VER = 0.4.3-neuro

gradlebuild :
	./gradlew -q build

build : clean
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
	java -jar build/libs/NeuroB-TrainingSetGeneration-$(VER).jar
	@echo "*****/ Training set generated"

distributedlibraryfile :
	@echo "***** Ensuring existence of LibraryIO.def in respective directories"
	java -jar build/libs/NeuroB-DistributeLibraryIODef-$(VER).jar
	@echo "*****/ Libraries made"