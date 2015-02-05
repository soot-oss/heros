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
package heros.alias;

import heros.alias.FlowFunction.Constraint;

public class DeltaConstraint<FieldRef> implements Constraint<FieldRef> {

	private SubAccessPath<FieldRef>[] delta;
	private AccessPath<FieldRef> accPathAtCallee;

	public DeltaConstraint(AccessPath<FieldRef> accPathAtCaller, AccessPath<FieldRef> accPathAtCallee) {
		this.accPathAtCallee = accPathAtCallee;
		delta = accPathAtCaller.getDeltaTo(accPathAtCallee);
	}

	@Override
	public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath, boolean sourceFact) {
		return accPath.addFieldReference(sourceFact, delta).mergeExcludedFieldReferences(accPathAtCallee);
	}

	@Override
	public boolean canBeAppliedTo(AccessPath<FieldRef> accPath) {
		return !accPath.isAccessInExclusions(delta);
	}

}
