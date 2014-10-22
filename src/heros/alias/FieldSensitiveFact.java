/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch, Johannes Späth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch, Johannes Späth - initial API and implementation
 ******************************************************************************/
package heros.alias;

import heros.solver.LinkedNode;

public interface FieldSensitiveFact<BaseValue, D> extends LinkedNode<FieldSensitiveFact<BaseValue, D>>{

	BaseValue getBaseValue();
	
	FieldReference[] getAccessPath();
	
	D cloneWithAccessPath(FieldReference... accessPath);
	
}
