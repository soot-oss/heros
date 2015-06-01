/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.fieldsens;

import heros.solver.PathEdge;

public abstract class ReturnConcretizationPathEdge<N,D> extends PathEdge<N, D> {


	public ReturnConcretizationPathEdge(D dSource, N target, D dTarget) {
		super(dSource, target, dTarget);
	}

	public void propagate() {
		_propagate(dSource, target, dTarget);
	}
	
	protected abstract void _propagate(D dSource, N target, D dTarget);
	
	@Override
	public PathEdge<N, D> copyWithTarget(D dTarget) {
		final ReturnConcretizationPathEdge<N, D> outer = this;
		return new ReturnConcretizationPathEdge<N, D>(dSource, target, dTarget) {

			@Override
			protected void _propagate(D dSource, N target, D dTarget) {
				outer._propagate(dSource, target, dTarget);
			}
		};
	}
	
	@Override
	public String toString() {
		return "ReturnConcretizationPathEdge "+super.toString();
	}
}
