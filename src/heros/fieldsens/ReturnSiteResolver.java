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
package heros.fieldsens;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.ReturnEdge;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class ReturnSiteResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, ReturnEdge<Field, Fact, Stmt, Method>> {

	private Stmt returnSite;
	private boolean propagated = false;
	private Fact sourceFact;
	private FactMergeHandler<Fact> factMergeHandler;

	public ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, Debugger<Field, Fact, Stmt, Method> debugger) {
		this(factMergeHandler, analyzer, returnSite, null, debugger, new AccessPath<Field>(), null);
		this.factMergeHandler = factMergeHandler;
		propagated = false;
	}

	private ReturnSiteResolver(FactMergeHandler<Fact> factMergeHandler, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, 
			Fact sourceFact, Debugger<Field, Fact, Stmt, Method> debugger, AccessPath<Field> resolvedAccPath, ReturnSiteResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer, resolvedAccPath, parent, debugger);
		this.factMergeHandler = factMergeHandler;
		this.returnSite = returnSite;
		this.sourceFact = sourceFact;
		propagated=true;
	}
	
	@Override
	public String toString() {
		return "<"+resolvedAccessPath+":"+returnSite+" in "+analyzer.getMethod()+">";
	}
	
	protected AccessPath<Field> getAccessPathOf(ReturnEdge<Field, Fact, Stmt, Method> inc) {
		return inc.usedAccessPathOfIncResolver.applyTo(inc.incAccessPath);
	}
	
	public void addIncoming(final WrappedFact<Field, Fact, Stmt, Method> fact, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, 
			Delta<Field> callDelta) {
		
		addIncoming(new ReturnEdge<Field, Fact, Stmt, Method>(fact, resolverAtCaller, callDelta));
	}
	
	protected void processIncomingGuaranteedPrefix(ReturnEdge<Field, Fact, Stmt, Method> retEdge) {
		if(propagated) {
			factMergeHandler.merge(sourceFact, retEdge.incFact);
		} 
		else {
			propagated=true;
			sourceFact = retEdge.incFact;
			analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
					new WrappedFact<Field, Fact, Stmt, Method>(retEdge.incFact, new AccessPath<Field>(), this)));
		}
	};
	
	protected void processIncomingPotentialPrefix(ReturnEdge<Field, Fact, Stmt, Method> retEdge) {
		log("Incoming potential prefix:  "+retEdge);
		resolveViaDelta(retEdge);
	};
	
	protected void log(String message) {
		analyzer.log("Return Site "+toString()+": "+message);
	}

	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, ReturnEdge<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return new ReturnSiteResolver<Field, Fact, Stmt, Method>(factMergeHandler, analyzer, returnSite, sourceFact, debugger, newAccPath, this);
	}
	
	public Stmt getReturnSite() {
		return returnSite;
	}
	
	private boolean isNullOrCallEdgeResolver(Resolver<Field, Fact, Stmt, Method> resolver) {
		if(resolver == null)
			return true;
		if(resolver instanceof CallEdgeResolver) {
			return !(resolver instanceof ZeroCallEdgeResolver);
		}
		return false;
	}
	
	private void resolveViaDelta(final ReturnEdge<Field, Fact, Stmt, Method> retEdge) {
		if(isNullOrCallEdgeResolver(retEdge.incResolver)) {
			resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge);
		} else {
			//resolve via incoming facts resolver
			Delta<Field> delta = retEdge.usedAccessPathOfIncResolver.applyTo(retEdge.incAccessPath).getDeltaTo(resolvedAccessPath);
			assert delta.accesses.length <= 1;
			retEdge.incResolver.resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {

				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
					if(resolver instanceof ZeroCallEdgeResolver) {
						ReturnSiteResolver.this.interest(((ZeroCallEdgeResolver<Field, Fact, Stmt, Method>) resolver).
								copyWithAnalyzer(ReturnSiteResolver.this.analyzer));
					} else {
						incomingEdges.add(retEdge.copyWithIncomingResolver(resolver, retEdge.incAccessPath.getDeltaTo(resolvedAccessPath)));
						ReturnSiteResolver.this.interest(ReturnSiteResolver.this);
					}
				}
				
				@Override
				public void canBeResolvedEmpty() {
					resolveViaDeltaAndPotentiallyDelegateToCallSite(retEdge);
				}
			});
		}			
	}

	private void resolveViaDeltaAndPotentiallyDelegateToCallSite(final ReturnEdge<Field, Fact, Stmt, Method> retEdge) {
		AccessPath<Field> inc = retEdge.usedAccessPathOfIncResolver.applyTo(retEdge.incAccessPath);
		if(!retEdge.callDelta.canBeAppliedTo(inc))
			return;
		
		final AccessPath<Field> currAccPath = retEdge.callDelta.applyTo(inc);
		if(resolvedAccessPath.isPrefixOf(currAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			incomingEdges.add(retEdge.copyWithIncomingResolver(null, retEdge.usedAccessPathOfIncResolver));
			interest(this);
		} else if(currAccPath.isPrefixOf(resolvedAccessPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			resolveViaCallSiteResolver(retEdge, currAccPath);
		}
	}

	protected void resolveViaCallSiteResolver(final ReturnEdge<Field, Fact, Stmt, Method> retEdge, AccessPath<Field> currAccPath) {
		if(isNullOrCallEdgeResolver(retEdge.resolverAtCaller)) {
			canBeResolvedEmpty();
		} else {
			retEdge.resolverAtCaller.resolve(new DeltaConstraint<Field>(currAccPath.getDeltaTo(resolvedAccessPath)), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
//					incomingEdges.add(retEdge.copyWithResolverAtCaller(resolver, retEdge.incAccessPath.getDeltaTo(getResolvedAccessPath())));
					ReturnSiteResolver.this.interest(resolver);
				}
				
				@Override
				public void canBeResolvedEmpty() {
					ReturnSiteResolver.this.canBeResolvedEmpty();
				}
			});
		}
	}
}
