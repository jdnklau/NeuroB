MACHINE definition_mch

VARIABLES
    x, y

DEFINITIONS
    SQR(i) == i*i;
    HUNDREDPI == 314;
    XX == x*x;

INVARIANT
    x : NATURAL &
    y : NATURAL &
    x < y * HUNDREDPI

INITIALISATION
    x := 0 || y := 3

OPERATIONS
  foo =
    BEGIN
        x := HUNDREDPI * SQR(y) + XX
    END
END
