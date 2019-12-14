/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *	 Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.edgefunc;

import heros.EdgeFunction;


public class AllBottom<V> implements EdgeFunction<V> {

	private final V bottomElement;

	public AllBottom(V bottomElement){
		this.bottomElement = bottomElement;
	}

	@Override
	public V computeTarget(V source) {
		return bottomElement;
	}

	@Override
	public EdgeFunction<V> composeWith(EdgeFunction<V> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		return secondFunction;
	}

	// The meet (greatest lower bound) of bottom with anything is bottom
	@Override
	public EdgeFunction<V> meetWith(EdgeFunction<V> otherFunction) {
		return this;
	}

	@Override
	public boolean equalTo(EdgeFunction<V> other) {
		if(other instanceof AllBottom) {
			@SuppressWarnings("rawtypes")
			AllBottom allBottom = (AllBottom) other;
			return allBottom.bottomElement.equals(bottomElement);
		}
		return false;
	}

	@Override
	public String toString() {
		return "allbottom";
	}

}
