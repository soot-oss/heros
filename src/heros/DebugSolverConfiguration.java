/*******************************************************************************
 * Copyright (c) 2015 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros;

public interface DebugSolverConfiguration {
	/**
	 * If true, the solver will record the edges produced by the flow function. These intermediate edges
	 * are not used by the solver but may be useful for debugging the flow functions.
	 */
	public boolean recordEdges();
}
