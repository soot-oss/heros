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

public class ZeroCallEdgeResolver<Field extends AccessPath.FieldRef<Field>, Fact, Stmt, Method> extends CallEdgeResolver<Field, Fact, Stmt, Method> {

	private ZeroHandler<Field> zeroHandler;

	public ZeroCallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, ZeroHandler<Field> zeroHandler) {
		super(analyzer);
		this.zeroHandler = zeroHandler;
	}

	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(zeroHandler.shouldGenerateAccessPath(constraint.applyToAccessPath(new AccessPath<Field>(), true)))
			callback.interest(analyzer, this);
	}
	
	@Override
	public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
	}
}
