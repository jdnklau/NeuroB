# Migration of a Predicate Database

This document outlines how to migrate a predicate trainig database into
a training set of a specified format.

## Use Case

Assuming an already existent predicate training database.
It might be interesting to use different machine learning algorithms
for comparison,
or use different approaches to similar problems.

The first case is relevant if, for example, the same data shall be used to
train a Decision Tree and a Convolutional Neural Network.
While for the first a CSV might be sensible,
the second would be better of trained on images.

The second case is relevant if, for example,
multiple classification algorithms shall be trained for different subsets of
backends, or if on the same data both a classification and a regression problem
is to be tackled.

## Usage: Regression Over a Specific Backend's Runtime

Assuming an existent predicate database is to be migrated for a
regression problem,
predicting the runtime for the Z3 Backend on a given predicate.

```java
// Preparation: Backends of interest, and database and training format
Backend[] backends = new Backend[]{
  new Z3Backend(),
};
JsonDbFormat dbFormat = new JsonDbFormat(backends);
CsvFormat trainingFormat = new CsvFormat(TheoryFeatures.featureDimension, 1);

// Migration step.
PredicateDbMigration migration = new PredicateDbMigration(dbFormat);
migration.migrate(
  jsonDbPath, targetDirectory, machineDirectory,
  TheoryFeatures::new,
  dbEntry -> new BackendClassification(
    dbEntry.getPredicate(),
    dbEntry.getBackendsUsed(),
    dbEntry.getResults()),
  trainingFormat
 );
```

The preparation part sets up the backends of interest.
In this case the array only contains a single backend, a `Z3Backend`.
The format is that of the predicate database form which the migration shall
take place. Here, the JsonDbFormat is used and initialised with the `backends`.

The migration step consists of two parts: initialisation, and execution.

```java
// Migration step: Initialisation
PredicateDbMigration migration = new PredicateDbMigration(dbFormat);
```

The initialisation of the migration expects the format of the
predicate database as its only argument.
Thus, the same migration instance can be reused for multiple migrations,
e.g. for translating the database into training data for regression on
ProB and Kodkod as well.

```java
// Migration step: Execution
migration.migrate(
  jsonDbPath, targetDirectory, machineDirectory, // Directories
  TheoryFeatures::new, // Feature format
  dbEntry -> new BackendClassification( // Label format
    dbEntry.getPredicate(),
    backends,
    dbEntry.getResults()),
  trainingFormat
 );
```

The execution step consists of calling `migrate` with its six arguments.

* Directories:
  The first three arguments are the directories needed for the migration.
  * `jsonDbPath`: The path to the predicate database, here a Json DB.
  * `targetDirectory`: Path to the directory in which the training data will be
    migrated to.
  * `machineDirectory`: Path to the B machines from which the database
    was sampled, e.g. `prob_examples`.
    This is necessary, as some features require to reload the respective
    machine file to be generatable, e.g. features that rely on the AST
    of a predicate.
    Note that the predicate database saves for each predicate the path
    relative to the one used during generation.
    For instance, if the database was generated for all B machines in
    `path/to/prob_examples`, the database contains paths relative to that.
    A database entry specifying `public_examples/foo.mch` as source
    would hence correspond to the machine located at
    `path/to/prob_examples/public_examples/foo.mch`.
* Feature format:
  A generator for the desired features. Most implemented features contain
  a subclass `Generator` for this. The line `TheoryFeatures::new`
  thus could also be written as
  `new TheoryFeatures.Generator()`.
  The generator has to conform the functional interface
  `PredicateFeatureGenerator`, thus any Java function `f` with the signature
  `Features f(BPredicate, MachineAccess) is sufficient.
* Label format:
  The same as with the feature format.
  Here, the `LabelTranslation` interface is implemented,
  thus any Java function `f` with the signature
  `PredicateLabelling f(PredDbEntry)` is sufficient.
  In this case, a lambda function is used, as no translation class was existent.
  In the lambda expression, a new `BackendClassification` is instantiated,
  taking the predicate, the desired backends,
  and a map of backends to their answers as arguments.
