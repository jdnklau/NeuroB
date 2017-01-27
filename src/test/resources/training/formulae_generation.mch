MACHINE
	formulae_generation
VARIABLES
	x, y, z
INVARIANT
	x : NATURAL & y : NAT & z : INT
INITIALISATION
	x,y,z := 1,1,1
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
