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
import heros.alias.FlowFunction.Constraint;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ReturnSiteResolver<Field extends AccessPath.FieldRef<Field>, Fact, Stmt, Method> extends Resolver<Field, Fact, Stmt, Method> {

	private Stmt returnSite;
	private AccessPath<Field> resolvedAccPath;
	private boolean propagated = false;
	private Set<ReturnEdge> incomingFacts;
	private Map<AccessPath<Field>, ReturnSiteResolver<Field, Fact, Stmt, Method>> nestedResolvers = Maps.newHashMap();
	private ReturnSiteResolver<Field, Fact, Stmt, Method> parent;

	public ReturnSiteResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite) {
		this(analyzer, returnSite, new AccessPath<Field>(), null);
	}

	private ReturnSiteResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt returnSite, AccessPath<Field> resolvedAccPath, ReturnSiteResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer);
		this.returnSite = returnSite;
		this.resolvedAccPath = resolvedAccPath;
		this.parent = parent;
		this.incomingFacts = Sets.newHashSet();
	}
	
	@Override
	public String toString() {
		return "<"+resolvedAccPath+":"+returnSite+">";
	}
	
	public AccessPath<Field> getResolvedAccessPath() {
		return resolvedAccPath;
	}
	
	public void addIncoming(final WrappedFact<Field, Fact, Stmt, Method> fact, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, 
			Delta<Field> callDelta) {
		
		addIncoming(new ReturnEdge(fact, resolverAtCaller, callDelta));
	}
	
	private void addIncoming(final ReturnEdge retEdge) {
		if(resolvedAccPath.isPrefixOf(retEdge.incAccessPath) == PrefixTestResult.GUARANTEED_PREFIX) {
			log("Incoming Edge "+retEdge);
			if(!incomingFacts.add(retEdge))
				return;

			interest(analyzer, this);
			
			for(ReturnSiteResolver<Field, Fact, Stmt, Method> nestedResolver : nestedResolvers.values())
				nestedResolver.addIncoming(nestedResolver.new ReturnEdge(retEdge.incFact, retEdge.incAccessPath, retEdge.incResolver, 
						retEdge.resolverAtCaller, retEdge.callDelta, retEdge.usedAccessPathOfIncResolver));

			if(!propagated) {
				propagated=true;
				analyzer.scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, 
						new WrappedFact<>(retEdge.incFact, new AccessPath<Field>(), this)));
			}
		}
		else if(retEdge.incAccessPath.isPrefixOf(resolvedAccPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			log("Incoming potential prefix:  "+retEdge);
			retEdge.resolveViaDelta();
		}
	}
	
	protected void log(String message) {
		analyzer.log("Return Site "+toString()+": "+message);
	}

	@Override
	public void resolve(Constraint<Field> constraint, final InterestCallback<Field, Fact, Stmt, Method> callback) {
		log("Resolve: "+constraint);
		if(!constraint.canBeAppliedTo(resolvedAccPath))
			return;
		
		AccessPath<Field> candidateAccPath = constraint.applyToAccessPath(resolvedAccPath, false);
		ReturnSiteResolver<Field, Fact, Stmt, Method> nestedResolver = getOrCreateNestedResolver(candidateAccPath);
		if(!nestedResolver.resolvedAccPath.equals(constraint.applyToAccessPath(resolvedAccPath, false)))
			throw new AssertionError();
		
		nestedResolver.registerCallback(callback);
	}
	
	private ReturnSiteResolver<Field, Fact, Stmt, Method> getOrCreateNestedResolver(AccessPath<Field> candidateAccPath) {
		if(resolvedAccPath.equals(candidateAccPath))
			return this;
		
		if(!nestedResolvers.containsKey(candidateAccPath)) {
			assert resolvedAccPath.getDeltaTo(candidateAccPath).accesses.length <= 1;
			final ReturnSiteResolver<Field, Fact, Stmt, Method> nestedResolver = new ReturnSiteResolver<>(analyzer, returnSite, candidateAccPath, this);
			nestedResolver.propagated = true;
			nestedResolvers.put(candidateAccPath, nestedResolver);

			for(ReturnSiteResolver<Field, Fact, Stmt, Method>.ReturnEdge retEdge: incomingFacts) {
				nestedResolver.addIncoming(nestedResolver.new ReturnEdge(retEdge.incFact, retEdge.incAccessPath, retEdge.incResolver, 
						retEdge.resolverAtCaller, retEdge.callDelta, retEdge.usedAccessPathOfIncResolver));
			}			
		}
		return nestedResolvers.get(candidateAccPath);
	}
	
	private class ReturnEdge {

		final Fact incFact;
		final Resolver<Field, Fact, Stmt, Method> resolverAtCaller;
		final Delta<Field> callDelta;
		final AccessPath<Field> incAccessPath;
		final Resolver<Field, Fact, Stmt, Method> incResolver;
		final Delta<Field> usedAccessPathOfIncResolver;

		public ReturnEdge(WrappedFact<Field, Fact, Stmt, Method> fact, Resolver<Field, Fact, Stmt, Method> resolverAtCaller,
				Delta<Field> callDelta) {
			this(fact.getFact(), fact.getAccessPath(), fact.getResolver(), resolverAtCaller, callDelta, Delta.<Field>empty());
		}
		
		private ReturnEdge(Fact incFact, AccessPath<Field> incAccessPath, Resolver<Field, Fact, Stmt, Method> incResolver, 
				Resolver<Field, Fact, Stmt, Method> resolverAtCaller, Delta<Field> callDelta, Delta<Field> usedAccessPathOfIncResolver) {
			this.incFact = incFact;
			this.incAccessPath = incAccessPath;
			this.incResolver = incResolver;
			this.resolverAtCaller = resolverAtCaller;
			this.callDelta = callDelta;
			this.usedAccessPathOfIncResolver = usedAccessPathOfIncResolver;
		}
		
		@Override
		public String toString() {
			return String.format("IncFact: %s%s, Delta: %s, IncResolver: <%s:%s>, ResolverAtCallSite: %s", incFact, incAccessPath, callDelta, usedAccessPathOfIncResolver, incResolver, resolverAtCaller);
		}
		
		public void resolveViaDelta() {
			if(incResolver == null || incResolver instanceof CallEdgeResolver) {
				resolveViaDeltaAndPotentiallyDelegateToCallSite();
			} else {
				//resolve via incoming facts resolver
				Delta<Field> delta = usedAccessPathOfIncResolver.applyTo(incAccessPath, true).getDeltaTo(resolvedAccPath);
				assert delta.accesses.length <= 1;
				incResolver.resolve(new DeltaConstraint<>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {

					@Override
					public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
						incomingFacts.add(new ReturnEdge(incFact, incAccessPath, resolver, resolverAtCaller, callDelta, incAccessPath.getDeltaTo(resolvedAccPath)));
						ReturnSiteResolver.this.interest(analyzer, resolver);
					}
					
					@Override
					public void canBeResolvedEmpty() {
						resolveViaDeltaAndPotentiallyDelegateToCallSite();
					}
				});
			}			
		}

		private void resolveViaDeltaAndPotentiallyDelegateToCallSite() {
			final AccessPath<Field> currAccPath = callDelta.applyTo(usedAccessPathOfIncResolver.applyTo(incAccessPath, true), true);
			if(resolvedAccPath.isPrefixOf(currAccPath) == PrefixTestResult.GUARANTEED_PREFIX) {
				incomingFacts.add(new ReturnEdge(incFact, incAccessPath, null, resolverAtCaller, callDelta, usedAccessPathOfIncResolver));
				ReturnSiteResolver.this.interest(analyzer, ReturnSiteResolver.this);
			} else if(currAccPath.isPrefixOf(resolvedAccPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
				resolveViaCallSiteResolver(currAccPath);
			}
		}

		protected void resolveViaCallSiteResolver(AccessPath<Field> currAccPath) {
			if(resolverAtCaller == null || resolverAtCaller instanceof CallEdgeResolver) {
				ReturnSiteResolver.this.canBeResolvedEmpty();
			} else {
				resolverAtCaller.resolve(new DeltaConstraint<>(currAccPath.getDeltaTo(resolvedAccPath)), new InterestCallback<Field, Fact, Stmt, Method>() {
					@Override
					public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
						incomingFacts.add(new ReturnEdge(incFact, incAccessPath, null, resolver, Delta.<Field>empty(), incAccessPath.getDeltaTo(resolvedAccPath)));
						ReturnSiteResolver.this.interest(analyzer, ReturnSiteResolver.this);
					}
					
					@Override
					public void canBeResolvedEmpty() {
						ReturnSiteResolver.this.canBeResolvedEmpty();
					}
				});
			}
		}
	}

	public Stmt getReturnSite() {
		return returnSite;
	}
}
