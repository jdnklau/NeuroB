# SQL Predicate Database

The SQLite database for predicates is split into the following tables and
relation ships.
The tables are described in detail below.

Notation:

* A "p" behind a key name indicates a primary key.
* An "f" behind a key name indicates a foreign key.

## `BMACHINE` Table

Key              | Type    | Description
-----------------|---------|-----------------------------------------
`sha512` p       | TEXT    | Sha512 value of the machine file
`path`           | TEXT    | Path in `prob_examples` repository
`formalism` f    | TEXT    | Formalism of the machine file

The `formalism` field corresponds to the `FORMALISM.name` entry.

## `FORMALISM` Table

Key                  | Type    | Description
---------------------|---------|-----------------------------------------
`name` p             | TEXT    | Name of the formalism

This table is intended to hold values like "CLASSICALB" or "EVENTB".

## `PREDICATE` Table

Key                | Type    | Description
-------------------|---------|---------------------------------------
`sha512` p         | TEXT    | Sha512 value of the predicate
`predicate`        | TEXT    | The B predicate in ASCII representation

## `PREDICATE_SOURCE` Table

Key                | Type    | Description
-------------------|---------|-----------------------------------------
`predicate_sha` f  | TEXT    | (Unique) Sha512 value of predicate in question
`machine_sha` f    | TEXT    | Sha512 value of machine the predicate is from

The foreign keys correspond to the primary keys of the `PREDICATE` and
`BMACHINE` tables respectively.

## `BACKEND` Table

Key                | Type    | Description
-------------------|---------|-----------------------------------------
`backend` p        | TEXT    | Constraint solving backend used by ProB

This table contains entries like "PROB" (ProB native backend), "KODKOD",
or "Z3".

## `PREDICATE_EVALUATION` Table

Key                  | Type    | Description
---------------------|---------|-----------------------------------------
`predicate_sha` f    | TEXT    | Sha512 of the predicate in question
`answer` f           | TEXT    | Answer return by evaluation
`runtime_ms`         | INTEGER | Runtime in milliseconds
`backend` f          | TEXT    | Backend used
`settings`           | TEXT    | Configuration of the backend

Table containing the evaluation results of the predicates run in
the backends.

## `ANSWER` Table

Key                  | Type    | Description
---------------------|---------|-----------------------------------------
`value`              | Type    | Semantic value of the answer

Table contains the following values:

* `VALID`: A solution could be found.
* `INVALID`: No solution could be found/a counter-example exists.
* `UNKNOWN`: Not decidable whether the predicate is `VALID` or `INVALID`.
* `TIMEOUT`: The backend run into a timeout whilst solving the predicate.
* `ERROR`: An error occurred whilst trying to solve the predicate.
  The type of error is not further specified and might for example be caused
  by an inability to translate the predicate correctly for the backend,
  or any ProB error.
