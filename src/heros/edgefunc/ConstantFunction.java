/*******************************************************************************
 * Copyright (c) 2019 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.edgefunc;

import heros.EdgeFunction;

public class ConstantFunction<V> implements EdgeFunction<V> {

	protected final V constantValue;

	public ConstantFunction(V constantValue) {
		this.constantValue = constantValue;
	}

	@Override
	public V computeTarget(V source) {
		return constantValue;
	}

	@Override
	public EdgeFunction<V> composeWith(EdgeFunction<V> secondFunction) {
		if(secondFunction.equalTo(this)) return this;
		
		return new ConstantFunction<V>(secondFunction.computeTarget(constantValue));
	}

	@Override
	public EdgeFunction<V> meetWith(EdgeFunction<V> otherFunction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equalTo(EdgeFunction<V> other) {
		if(other instanceof ConstantFunction) {
			@SuppressWarnings("rawtypes")
			ConstantFunction otherFunction = (ConstantFunction) other;
			return otherFunction.constantValue.equals(constantValue);			
		}
		return false;
	}

}
