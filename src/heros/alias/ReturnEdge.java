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
import heros.alias.AccessPath.PrefixTestResult;

class ReturnEdge<Field, Fact, Stmt, Method> {

	final Fact incFact;
	final Resolver<Field, Fact, Stmt, Method> resolverAtCaller;
	final Delta<Field> callDelta;
	final AccessPath<Field> incAccessPath;
	final Resolver<Field, Fact, Stmt, Method> incResolver;
	final Delta<Field> usedAccessPathOfIncResolver;

	public ReturnEdge(WrappedFact<Field, Fact, Stmt, Method> fact, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller,
			Delta<Field> callDelta) {
		this(fact.getFact(), fact.getAccessPath(), fact.getResolver(), resolverAtCaller, callDelta, Delta.<Field>empty());
	}
	
	private ReturnEdge(Fact incFact, 
			AccessPath<Field> incAccessPath, 
			Resolver<Field, Fact, Stmt, Method> incResolver, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, 
			Delta<Field> callDelta, 
			Delta<Field> usedAccessPathOfIncResolver) {
		this.incFact = incFact;
		this.incAccessPath = incAccessPath;
		this.incResolver = incResolver;
		this.resolverAtCaller = resolverAtCaller;
		this.callDelta = callDelta;
		this.usedAccessPathOfIncResolver = usedAccessPathOfIncResolver;
	}
	
	public ReturnEdge<Field, Fact, Stmt, Method> copyWithIncomingResolver(
			Resolver<Field, Fact, Stmt, Method> incResolver, Delta<Field> usedAccessPathOfIncResolver) {
		return new ReturnEdge<>(incFact, incAccessPath, incResolver, resolverAtCaller, callDelta, usedAccessPathOfIncResolver);
	}
	
	public ReturnEdge<Field, Fact, Stmt, Method> copyWithResolverAtCaller(
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, Delta<Field> usedAccessPathOfIncResolver) {
		return new ReturnEdge<>(incFact, incAccessPath, null, resolverAtCaller, callDelta, usedAccessPathOfIncResolver);
	}
	
	@Override
	public String toString() {
		return String.format("IncFact: %s%s, Delta: %s, IncResolver: <%s:%s>, ResolverAtCallSite: %s", incFact, incAccessPath, callDelta, usedAccessPathOfIncResolver, incResolver, resolverAtCaller);
	}
	
	
}