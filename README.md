# NeuroB

An artificial deep learning framework for ProB.

With NeuroB,
one can set up neural networks an train them on problems related to
the B method.
Using ProB2 to load B machines and to access their state spaces,
one can create training sets from B predicates or whole B machines
and set up classification or regression tasks over them.

## Requirements

- Java 1.8+
- [Z3 Theorem Prover](https://github.com/Z3Prover/z3), version 4.6.0

## Getting the development started

All dependencies reside in the `build.gradle` file.
For IntelliJ, one can simply import it:

```bash
idea build.gradle
```

### Modules contained in the repository

- `core`: NeuroB library itself and all its code,
- `cli`: Command line program to use NeuroB from command line.

### Coding conventions

- indentation
  - 4 spaces
  - hanging indent: 4 spaces as well
- maximal line length: 80 characters
- no wildcard imports
- src/ layout per module:
  - src/main/java: java source code
  - src/main/resources: non-java resource files
  - src/test/java: unit tests
  - src/it/java: integration tests
  - src/it/resources: integration test resources (B machines and alike)
