# About this document
This document holds the results reached in development of NeuroB since 11th November 2016, and lists them with timestamp, project version, and commit id to get an overview.

Each approach to classify over predicates is listed independently of others and gets briefly described.

At the end of the file is an overview of the different feature classes used.


# Results
## KodKod only prediction
This approach tries to predict whether a predicate is decidable by KodKod or not.

Uses Predicate Features.

Date      | Version | commit | Accuracy | Precision | Recall | F1 Score
----------|:-------:|--------|---------:|----------:|-------:|---------:
2016-11-11| 0.16.0  | 2640eec|   0.9112 |    0.8311 | 0.6287 |   0.7159
2016-11-11| 0.16.0~ | 1d2a714|   0.8947 |    0.7265 | 0.5577 |   0.631

Further notes:
- The net serves as a comparison to the bigger nets which classify between multiple solvers.
- Before 7cdebdd there were only 15 features, but more were added as some examples gave the same feature vector but needed to be labeled differently.
- wrapping the epoch loop around the whole training step instead of current batch (1d2a714) results in slightly worse results, but should actually be more accurate

## 3-way Solver Classification
This approach aims to predict for ProB, KodKod, and ProB+Z3 whether each can individually decide a predicate or not. Returns a 3-dimensional vector of the form `[y_1, y_2, y_3]`.

Uses Predicate Features.

Date      | Version | commit | Accuracy | Precision | Recall | F1 Score
----------|:-------:|--------|---------:|----------:|-------:|---------:
-         |        -|       -|         -|          -|       -|        -
(No data recorded yet)


# Feature classes
## Predicate Features
Reads points of interest from predicates, and counts them.
As of commit 2640eec those features are:
1. Number of arithmetic operators
2. Number of comparison operators (`>`, `<`, `=`, ...)
3. Number of universal quantification signs
4. Number of existential quantification signs
5. Number of conjunctions
6. Number of disjunctions
7. Number of negations
8. Number of set operations (union, ...)
9. Number of set memberships
10. Number of functions used
11. Number of relational operators
12. Number of unique identifiers
13. Number of identifiers in finite domains
14. Number of identifiers in infinite domains
15. Number of identifiers in domains of unknown size
16. Number of implications
17. Number of equivalences

## Code Portfolio
The code is transformed into a square image of fixed size, that hold coding patterns used. This is meant to serve as a comparison to manually crafted features like Predicate Features.
