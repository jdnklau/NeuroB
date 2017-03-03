# Shortcut to run the Cli of NeuroB
RUNCLI = ./build/install/NeuroB/bin/NeuroB
EXAMPLES = examples/prob_examples/public_examples

all : neurob dev unzip_examples distributedlibraryfile 
	@echo "Done."

examples/prob_examples :
	@echo "***** Cloning prob_examples; if this fails, you do not have permission"
	@echo "      You can still use NeuroB though"
	@git clone git@tuatara.cs.uni-duesseldorf.de:prob/prob_examples.git examples/prob_examples/
	
unzip_examples : examples/prob_examples
	@echo "***** unzipping eventb machines"
	@find examples/prob_examples/public_examples/ -iname *.zip -exec unzip -uo -d "{}_unpacked" {} \;

neurob :
	@echo "***** Create NeuroB binary"
	@./gradlew installDist
	@echo "*****/ Created binary"

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

predicatedump : distributedlibraryfile
	@$(RUNCLI) pdump -dir $(EXAMPLES)
	@echo "Ensuring the termination of all KodKod processes... the hard way:"
	pkill -u $(USER) -f probkodkod

alltrainingsets : distributedlibraryfile
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf solclass -solver prob
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf solclass -solver kodkod
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf solsel
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predf soltime
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi solclass -solver prob -size 32
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi solclass -solver kodkod -size 32
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi solsel -size 32
	@$(RUNCLI) trainingset -dir $(EXAMPLES) -net predi soltime -size 32

distributedlibraryfile : examples/prob_examples
	@echo "***** Ensuring existence of LibraryIO.def in respective directories"
	@$(RUNCLI) libraryIODef -dir examples/prob_examples/public_examples/B/
	@echo "*****/ Libraries made"
