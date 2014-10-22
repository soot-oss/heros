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

public class SummaryEdge<D, N> {

	private D sourceFact;
	private N targetStmt;
	private D targetFact;
	
	public SummaryEdge(D sourceFact, N targetStmt, D targetFact) {
		this.sourceFact = sourceFact;
		this.targetStmt = targetStmt;
		this.targetFact = targetFact;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceFact == null) ? 0 : sourceFact.hashCode());
		result = prime * result + ((targetFact == null) ? 0 : targetFact.hashCode());
		result = prime * result + ((targetStmt == null) ? 0 : targetStmt.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SummaryEdge))
			return false;
		SummaryEdge other = (SummaryEdge) obj;
		if (sourceFact == null) {
			if (other.sourceFact != null)
				return false;
		} else if (!sourceFact.equals(other.sourceFact))
			return false;
		if (targetFact == null) {
			if (other.targetFact != null)
				return false;
		} else if (!targetFact.equals(other.targetFact))
			return false;
		if (targetStmt == null) {
			if (other.targetStmt != null)
				return false;
		} else if (!targetStmt.equals(other.targetStmt))
			return false;
		return true;
	}

	public D getSourceFact() {
		return sourceFact;
	}
	
	public D getTargetFact() {
		return targetFact;
	}
	
	public N getTargetStmt() {
		return targetStmt;
	}
	
}
