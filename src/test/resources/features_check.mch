MACHINE
	features_check
VARIABLES
	x, y, z,
	a, b, c
INVARIANT
	x : NATURAL & y : INTEGER & z : NATURAL & z < 20 &
	a : NAT & b : NAT1 & a < 7 & c : INT
	&
	# y . (y < x)
	&
	! z . (z < 15 => x > 3)
INITIALISATION
	x,y,z,a,b,c := 1,1,1,1,1,1
END
