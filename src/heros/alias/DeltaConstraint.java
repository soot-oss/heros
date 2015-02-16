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

import heros.alias.AccessPath.Delta;
import heros.alias.FlowFunction.Constraint;

public class DeltaConstraint<FieldRef extends AccessPath.FieldRef<FieldRef>> implements Constraint<FieldRef> {

	private Delta<FieldRef> delta;

	public DeltaConstraint(AccessPath<FieldRef> accPathAtCaller, AccessPath<FieldRef> accPathAtCallee) {
		delta = accPathAtCaller.getDeltaTo(accPathAtCallee);
	}

	@Override
	public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath, boolean sourceFact) {
		if(accPath.hasResolver()) {
			return delta.applyTo(accPath, sourceFact).decorateResolver(this);
		}
		else
			return delta.applyTo(accPath, sourceFact);
	}

	@Override
	public boolean canBeAppliedTo(AccessPath<FieldRef> accPath) {
		return delta.canBeAppliedTo(accPath);
	}

}
