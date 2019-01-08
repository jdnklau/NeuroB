# Json Predicate database

Json format used to store Predicates which where generated over B machines.

```json
// Example json
{
  "path/to/source/machine.mch":
  {
    "sha512": "deadbeef",
    "formalism": "CLASSIALB",
    "gathered-predicates":
    [
      {
        "predicate": "x > 0 & y > x",
        "sha512": "facedeed",
        "probcli":
        {
          "version":"1.8.3-beta3",
          "revision":"ac234c49933b20a2e82af965f8576b64a1063113"
        },
        "results":
        {
          "ProB":
          {
            "time-in-ns": 123456,
            "answer": "VALID",
            "timeout-in-ns": 2500000000
          },
          "Z3": { /* ... */ },
          /* further backends ... */
        }
      },
      /* further predicates ... */
    ]
  }
}
```

Each json database file contains data of exactly one B machine.
The data for each machine are

* `sha512`: The sha512 hash value of the original machine file from which the data were
  generated.
* `formalism`: Either `CLASSICALB` or `EVENTB`.
  The value corresponds to the formalism of the source file and matches
  with the entries of the `de.hhu.stups.neurob.core.api.MachineType` enum.
  Extension by further formalisms supported in the future, or a `UNKNOWN` value
  are possible.
* `gathered-predicates`: List of predicate objects gathered from the source,
  containing evaluation over various backends (see below).

Also each file only should refer to one B machine, it is technically possible
to have further machines matching the above pattern in the file.

## Format of gathered predicates

The gathered predicates are stored as objects themselves.
They contain the following attributes:

* `predicate`: The predicate itself.
* `sha512`: Hash value of the predicate.
* `probcli`: An object containing data about the ProB Cli version.
  * `version`: The version string of the Cli.
  * `revision`: Git sha of commit from which the Cli was build.
* `results`: An object itself, storing information of solving time
  and answer on a per backend basis.
  For each backend the predicate was tried to solve over,
  this object contains an attribute which maps again to an object
  containing the following data:
    * `answer`: The answer the backend provided (see answer values below).
    * `time-in-ns`: Time it took the backend to solve the predicate in
      nanoseconds.
    * `timeout-in-ns`: The timeout given for the backend to
      solve the predicate.

## Answer Values

Running a predicate through a backend to solve it may yield one of the
following answer types:

* `VALID`: A solution could be found.
* `INVALID`: No solution could be found/a counter-example exists.
* `UNKNOWN`: Not decidable whether the predicate is `VALID` or `INVALID`.
* `TIMEOUT`: The backend run into a timeout whilst solving the predicate.
* `ERROR`: An error occurred whilst trying to solve the predicate.
  The type of error is not further specified and might for example be caused
  by an inability to translate the predicate correctly for the backend,
  or any ProB error.
