# Enter project version here:
VER = 0.3.2

gradle :
	./gradlew build

build : clean
	# ***** Build .jar files
	./gradlew buildJars
	
clean :
	# ***** Clean gradle
	./gradlew clean

# how to run stuff
trainingset : 
	java -jar build/libs/NeuroB-TrainingSetGeneration-$(VER).jar