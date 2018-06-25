MACHINE
	formulae_generation
CONSTANTS
	n, m
PROPERTIES
	n = 1 & m = 2*n
VARIABLES
	x, y, z
INVARIANT
	x : NATURAL & y : NAT & z : INT
INITIALISATION
	x,y,z := 1,1,1
ASSERTIONS
	y > z or x > z
OPERATIONS
	incx =
		PRE x = y & z < 20 THEN
			x := x+1;
			z := z+1
		END;
	incy =
		PRE y < x & z < 20 THEN
			y := y+2;
			z := z+1
		END;
	sqrx =
		PRE x < y THEN
			x := x*x;
			z := z+1
		END;
	reset = 
		PRE z >=20 or x > 1000 THEN
			x,y,z:=1,1,1
		END
END
