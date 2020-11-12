MACHINE cbc_solve_test

VARIABLES
    x

INVARIANT
    x : 1..10

INITIALISATION
    x := 4

OPERATIONS
  inc =
    BEGIN
        x := x+1
    END;

  dec =
    BEGIN
        x := x-1
    END
END
