/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.alias;

public class IncomingEdge<D, N> {

	private D calleeSourceFact;
	private N callSite;
	private D callerSourceFact;
	private D callerCallSiteFact;
	
	public IncomingEdge(D calleeSourceFact, N callSite, D callerSourceFact, D callerCallSiteFact) {
		super();
		this.calleeSourceFact = calleeSourceFact;
		this.callSite = callSite;
		this.callerSourceFact = callerSourceFact;
		this.callerCallSiteFact = callerCallSiteFact;
	}
	
	public D getCalleeSourceFact() {
		return calleeSourceFact;
	}
	
	public D getCallerCallSiteFact() {
		return callerCallSiteFact;
	}
	
	public D getCallerSourceFact() {
		return callerSourceFact;
	}
	
	public N getCallSite() {
		return callSite;
	}
	
	@Override
	public String toString() {
		return "[IncEdge CSite:"+callSite+", Caller-Edge: "+callerSourceFact+"->"+callerCallSiteFact+",  CalleeFact: "+calleeSourceFact+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
		result = prime * result + ((calleeSourceFact == null) ? 0 : calleeSourceFact.hashCode());
		result = prime * result + ((callerCallSiteFact == null) ? 0 : callerCallSiteFact.hashCode());
		result = prime * result + ((callerSourceFact == null) ? 0 : callerSourceFact.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IncomingEdge))
			return false;
		IncomingEdge other = (IncomingEdge) obj;
		if (callSite == null) {
			if (other.callSite != null)
				return false;
		} else if (!callSite.equals(other.callSite))
			return false;
		if (calleeSourceFact == null) {
			if (other.calleeSourceFact != null)
				return false;
		} else if (!calleeSourceFact.equals(other.calleeSourceFact))
			return false;
		if (callerCallSiteFact == null) {
			if (other.callerCallSiteFact != null)
				return false;
		} else if (!callerCallSiteFact.equals(other.callerCallSiteFact))
			return false;
		if (callerSourceFact == null) {
			if (other.callerSourceFact != null)
				return false;
		} else if (!callerSourceFact.equals(other.callerSourceFact))
			return false;
		return true;
	}
	
	
}
