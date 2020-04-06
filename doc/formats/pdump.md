# Predicate Dump

This is the legacy format used in the corresponding Master's Thesis.
The format of the individual files is pretty straight forward,
given below in BNF.

    <Result File> ::= <Source annotation> <Result List>
    <Source annotation> ::= '#source:' <path to source machine file> '\n'
    <Result List> ::= <Result> | <Result> '\n' <Result List>
    <Result>      ::= <Time> ',' <Time> ',' <Time> ',' <Time> ':' <predicate>
    <Time>        ::= '-1' | <needed time>

The annotated source path corresponds to the path of the source machine file
in the `prob_examples` repository.

The stated time records correspond, in this order, to

1. ProB,
2. KodKod,
3. Z3,
4. SMT\_SUPPORTED\_INTERPRETER.

They are the mean of three runs per predicate, for each solver, and stated in
nano seconds in double precision. `-1` indicates that the respective solver
could *not* decide the predicate, whether due to timeout or error is not
captured here.

## Example File

    #source:examples/prob_examples/public_examples/path/to/source.mch
    -1,-1,1.23456789E8,2.3456789E8:(x<y) & (y<x)
    -1,-1,1.23456789E8,2.3456789E8:(x<y) <=> (y<x)
