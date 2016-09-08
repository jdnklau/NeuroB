gradle :
	./gradlew build

build : clean
	echo "***** Build .jar files"
	./gradlew buildJars
	
clean :
	echo "***** Clean gradle"
	./gradlew clean

# how to run stuff
trainingset : 
	java -jar build/libs/NeuroB-TrainingSetGeneration-0.3.1.jar