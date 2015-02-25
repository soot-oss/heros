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

public interface InterestCallback<Field extends AccessPath.FieldRef<Field>, Fact, Stmt, Method> {

	void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver);
	
//	void partiallyResolved(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver, AccessPath<Field> resolvedAccPath);
	
	void canBeResolvedEmpty();
}
