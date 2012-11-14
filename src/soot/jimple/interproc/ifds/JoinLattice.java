package soot.jimple.interproc.ifds;

/**
 * This class defines a lattice in terms of its top and bottom elements
 * and a join operation. 
 *
 * @param <V> The domain type for this lattice.
 */
public interface JoinLattice<V> {
	
	V topElement();
	
	V bottomElement();
	
	V join(V left, V right);

}
