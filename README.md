# NeuroB
An artificial neural net framework for ProB. 

With NeuroB one can (at least when it is finished)
make use of a neural net to (hopefully) increase the performance in 
model checking with ProB or simply to enhance the solver selection for 
constraint problems or alike.



## Requirements
- Java 1.8+
- Make sure to have [the Z3 Theorem Prover](https://github.com/Z3Prover/z3) installed.



## Getting the development started
Clone the repository and change into the NeuroB main directory.
In the following stands the assumption, that this is the active directory.
```
# load dependencies (Gradle), set up project (Eclipse), 
# create NeuroB binary (cli), clone prob_examples
$ make

# creates the cli only; use this if you pulled new changes
$ make neurob
```
**Note**: cloning the prob_examples fails, if you lack permission to access
them. NeuroB is still usable without them, but as it operates on 
B and EventB machines, you will need your own example files.

After this, `./build/install/NeuroB/bin/NeuroB` should be accessible:
```
# List arguments to pass to the NeuroBCli
$ ./build/install/NeuroB/bin/NeuroB help

# Alternatively on Windows:
$ ./build/install/NeuroB/bin/NeuroB.bat help
```

This is the NeuroB Cli, that allows for easy testing and toying around. 
To use NeuroB, you will need a training set.



## Using the Cli
### Generating a training set
(In the following, the prob_examples are assumed to be present)
```
# generate the training set for ProB classifier
$ make trainingset

# make target above simply substitutes
$ ./build/install/NeuroB/bin/NeuroB trainingset -dir examples/prob_examples/
```
Call `./build/install/NeuroB/bin/NeuroB help` to check for alternative calls,
and how to generate training sets for different problems.


### Training sets for different yet similar problems
NeuroB supports different problem tasks.
It defaults to a classification problem, deciding between those predicates
decidable by ProB, and those which are not. But by its dynamic nature,
one can create its own problem task(s) and still use NeuroB.

With regards to problems, that are related in nature, but use different 
training sets, NeuroB offers more general data to derive the final
training set from: Predicate dumps.

#### Example
You are interested in
- classification for ProB decidability only
- classification for KodKod decidability only
- regression of decision time for both, ProB and KodKod

Instead of having to run the training set generation three times over 
all our sample machines, simply run
```
$ make predicatedump
```
and work with the generated .pdump files.
Those can be translated into the desired training sets without having the
solvers to actually solve the same predicates multiple times.
```
$ ./build/install/NeuroB/bin/NeuroB pdump -translate training_data/PredicateDump/
```
Call `./build/install/NeuroB/bin/NeuroB help` to check for alternative calls,
and how to generate training sets for different problems.


### Toying around
As mentioned above, 
`$ ./build/install/NeuroB/bin/NeuroB help` displays a list of commands for 
the NeuroB Cli. Here are some examples,
with `./build/install/NeuroB/bin/NeuroB` substituted as simply `NeuroB`:
```
# Generate training set from machine files in specified directory
$ NeuroB trainingset -dir /path/to/directory/

# Generate training data from single file
$ NeuroB trainingset -file /path/to/file.mch

# After training set generation, get an overview of the training data
$ NeuroB trainingset -analyse -dir /path/to/training/data/
```


## Using NeroB in Java code
tbd
