/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros;

/**
 * This class defines a lattice in terms of its top and bottom elements
 * and a meet operation. This is meant to be a complete lattice, with a unique top and bottom element. 
 *
 * @param <V> The domain type for this lattice.
 */
public interface MeetLattice<V> {
	
	/**
	 * Returns the unique top element of this lattice.
	 */
	V topElement();
	
	/**
	 * Returns the unique bottom element of this lattice.
	 */
	V bottomElement();
	
	/**
	 * Computes the meet of left and right. Note that <pre>meet(top,x) = meet(x,top) = x</pre> and
	 * <pre>meet(bottom,x) = meet(x,bottom) = bottom</pre>. 
	 */
	V meet(V left, V right);

}
