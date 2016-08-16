# List of possible features
This list is directly taken from "Machine Learning and Automated Theorem Proving" from James P. Bridge of University of Cambridge.

## Details of features
### Initial feature set
In the following U is set of unprocessed clauses, and P that of processed. Clause length is number of literals, depth a degree of nested teams. Weight is based on a weighting scheme used in term ordering.

1. Proportion of the total number of generated clauses that are kept (i.e. are not discarded as being trivial).
2. The “Sharing Factor”, a measure of the number of terms which are shared between different clauses. The sharing factor is provided as a function within the E source code.
3. Proportion of the total clauses that are in P (i.e. have been processed).
4. The ratio of the size of multi-set U to its original size (the original size being the number of axioms in the original theorem).
5. The ratio of the longest clause in P to the longest clause in the original axioms.
6. The ratio of the average clause length in P to the average axiom clause length.
7. The ratio of length of the longest clause in U to the longest axiom clause length.
8. The ratio of the average clause length in U to the average axiom clause length.
9. The ratio of the maximum clause depth in P to the maximum axiom clause depth.
10. The ratio of the average clause depth in P to the average axiom clause depth.
11. The ratio of the maximum clause depth in U to the maximum axiom clause depth.
12. The ratio of the average clause depth in U to the average axiom clause depth.
13. The ratio of the maximum clause standard weight in P to the maximum axiom clause standard weight.
14. The ratio of the average clause standard weight in P to the average axiom clause standard weight.
15. The ratio of the maximum clause standard weight in U to the maximum axiom clause standard weight.
16. The ratio of the average clause standard weight in U to the average axiom clause standard weight.

## Extended feature set
### static feature set
Measured for the clauses in the negated
conjecture and associated axioms prior to the proof search.

1. Fraction of Clauses that are Unit Clauses (i.e. clauses containing a single literal).
2. Fraction of Clauses that are Horn Clauses (i.e. clauses containing no more than one positive literal).
3. Fraction of Clauses that are Ground Clauses (i.e. clauses with no variables).
4. Fraction of Clauses that are Demodulators.
5. Fraction of Clauses that are Re-write Rules (see background chapter).
6. Fraction of Clauses that are purely positive
7. Fraction of Clauses that are purely negative
8. Fraction of Clauses that are mixed positive and negative
9. Maximum Clause Length
10. Average Clause Length
11. Maximum Clause Depth
12. Average Clause Depth
13. Maximum Clause Weight
14. Average Clause Weight

### dynamic feature set
Note, the dynamic features are measured at a point of the proof search when one hundred selected clauses have been processed.

Note that in the following context sr count, factor count and resolv count are counters maintained by E which were embodied into features as described.

1. Proportion of Generated Clauses that are kept (clauses that are subsumed or are trivial are discarded).
2. Sharing Factor (measure of the number of shared terms - the E theorem prover provides a function for calculating the sharing factor and Stephan Schulz, the author of
E, has indicated in private correspondence that he’d noted that sharing factor seems to correlate with how quickly some proofs are found). Note that E does not store separate copies of shared terms, this increases efficiency as terms need only be rewritten once.
3. Ratio of Number of Clauses in P/Number in (P + U), i.e. the size of the saturated clause set relative to the total number of clauses in the current proof state.
4. Size of U/Original Size of U (ie the number of Axioms). This should be a measure as to how rapidly the number of generated clauses has grown given that the measure is taken after a fixed number of clauses has been selected as the given clause.
5. Ratio of Longest Clause Length in P to Longest Axiom Clause Length.
6. Ratio of Average Clause Length in P to Average Axiom Clause Length.
7. Ratio of Longest Clause Length in U to Longest Axiom Clause Length.
8. Ratio of Average Clause Length in U to Average Axiom Clause Length.
9. Ratio of Maximum Clause Depth in P to Maximum Axiom Clause Depth.
10. Ratio of Average Clause Depth in P to Average Axiom Clause Depth.
11. Ratio of Maximum Clause Depth in U to Maximum Axiom Clause Depth.
12. Ratio of Average Clause Depth in U to Average Axiom Clause Depth.
13. Ratio of Maximum Clause Standard Weight in P to Maximum Axiom Clause Standard Weight.
14. Ratio of Average Clause Standard Weight in P to Average Axiom Clause Standard Weight.
15. Ratio of Maximum Clause Standard Weight in U to Maximum Axiom Clause Standard Weight.
16. Ratio of Average Clause Standard Weight in U to Average Axiom Clause Standard Weight.
17. Ratio of the number of trivial clauses to the total number of processed clauses. (Trivial clauses are those that are trivially true, because they either contain a literal and its negation, or they contain a literal of the form t = t.)
18. Ratio of the number of forward subsumed clauses to the total number of processed clauses.
19. Ratio of the number of non-trivial clauses to the total number of processed clauses, this should be effectively the same as feature 17 above.
20. Ratio of the number of other redundant clauses to the total number of processed clauses.
21. Ratio of the number of non-redundant deleted clauses to the total number of processed clauses.
22. Ratio of the number of backward subsumed clauses to the total number of processed clauses.
23. Ratio of the number of backward rewritten clauses to the total number of processed clauses.
24. Ratio of the number of backward rewritten literal clauses to the total number of processed clauses.
25. Ratio of the number of generated clauses to total number of processed clauses.
26. Ratio of the number of generated literal clauses to the total number of processed clauses.
27. Ratio of the number of generated non-trivial clauses to the total number of processed clauses.
28. Ratio context sr count to the total number of processed clauses (clauses generated
from a contextual or top level simplify-reflect also known as contextual literal cutting or
subsumption resolution inference step - see the E user guide for details).
29. Ratio of paramodulations to the total number of processed clauses.
30. Ratio of factor count (the number of factors found) to the total number of processed clauses.
31. Ratio of resolv count (resolvant count) to the total number of processed clauses.
32. Fraction of total clauses in U that are Unit.
33. Fraction of total clauses in U that are Horn.
34. Fraction of total clauses in U that are Ground Clauses.
35. Fraction of total clauses in U that are demodulators.
36. Fraction of total clauses in U that are Re-write Rules.
37. Fraction of total clauses in U that contain only positive literals.
38. Fraction of total clauses in U that contain only negative literals.
39. Fraction of total clauses in U that contain both positive and negative literals.
