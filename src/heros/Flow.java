/*******************************************************************************
 * Copyright (c) 2017 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros;

public interface Flow<N,D, V> {
	public void nonIdentityCallToReturnFlow(D d2, N callSite, D d3, N returnSite, D d1, EdgeFunction<V> func);
	public void nonIdentityReturnFlow(N exitStmt,D d2, N callSite, D d3, N returnSite, D d1, EdgeFunction<V> func);
}
