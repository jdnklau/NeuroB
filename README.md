# NeuroB
A neural net framework for ProB. With NeuroB one can (at least when it is finished) make use of a neural net to (hopefully) increase the performance in model checking with ProB or simply to enhance the solver selection for constraint problems or alike.

## Requirements
- Java 1.8+
- Make sure to have [the Z3 Theorem Prover](https://github.com/Z3Prover/z3) installed.

## Getting the development started
Clone the repository and change into the NeuroB main directory.
In the following stands the assumption, that this is the active directory.
```
# Build the mandatory files with Gradle and a NeuroB binary (Cli)
$ make
$ make install
```
After this, `build/install/NeuroB/bin/NeuroB` should be accessible:
```
# List arguments to pass to the NeuroBCli
$ ./build/install/NeuroB/bin/NeuroB help

# Alternatively on Windows:
$ ./build/install/NeuroB/bin/NeuroB.bat help
```

## Generating the training set
Although this repository provides everything needed to go for it, it is advisable to clone the prob_examples repository into `/NeuroB_dir/prob_examples/`.
As a neural net needs to be trained, the public prob examples serve as raw data to generate the training set. To generate it, simply type the following command (the execution takes some hours):
```
$ make trainingset
```
This **assumes** the prob_examples are present.

## Toying around
As mentioned above, `$ ./build/install/NeuroB/bin/NeuroB help` displays a list of commands for the NeuroB Cli. Here are some examples, with `./build/install/NeuroB/bin/NeuroB` substituted as simply `NeuroB`:
```
# Generate training set from .ch files in specified directory
$ NeuroB trainingset -dir /path/to/directory/

# Generate training data from single file
$ NeuroB trainingset -file /path/to/file.mch

# After training set generate, get an overview of the training data
$ NeuroB trainingset -analyse -dir /path/to/training/data/
```

## Using the fatjar
```
# Here you go
$ make jar
```
This generates `build/libs/NeuroB-cli-$(VERSION).jar`, that takes the same command line arguments as mentioned (most notably: `help`). `$(VERSION)` hereby references the NeuroB version (obviously), and if you have multiple versions of it, use the latest one (again, obviously).
