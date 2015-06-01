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

public class CallConcretizationPathEdge<M, N,D> extends PathEdge<N, D> {

	private M calleeMethod;
	private D calleeSourceFact;

	public CallConcretizationPathEdge(D dSource, N target, D dTarget, M calleeMethod, D calleeSourceFact) {
		super(dSource, target, dTarget);
		this.calleeMethod = calleeMethod;
		this.calleeSourceFact = calleeSourceFact;
	}

	public M getCalleeMethod() {
		return calleeMethod;
	}

	public D getCalleeSourceFact() {
		return calleeSourceFact;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((calleeMethod == null) ? 0 : calleeMethod.hashCode());
		result = prime * result + ((calleeSourceFact == null) ? 0 : calleeSourceFact.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof CallConcretizationPathEdge))
			return false;
		CallConcretizationPathEdge other = (CallConcretizationPathEdge) obj;
		if (calleeMethod == null) {
			if (other.calleeMethod != null)
				return false;
		} else if (!calleeMethod.equals(other.calleeMethod))
			return false;
		if (calleeSourceFact == null) {
			if (other.calleeSourceFact != null)
				return false;
		} else if (!calleeSourceFact.equals(other.calleeSourceFact))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ConcretizationPathEdge "+super.toString()+ " registers interest at "+calleeMethod+" in "+calleeSourceFact;
	}
}
