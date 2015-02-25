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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import heros.alias.AccessPath.Delta;
import heros.alias.AccessPath.PrefixTestResult;
import heros.alias.FlowFunction.Constraint;

public class ControlFlowJoinResolver<Field extends AccessPath.FieldRef<Field>, Fact, Stmt, Method> extends Resolver<Field, Fact, Stmt, Method> {

	private boolean recursiveLock = false;
	private Stmt joinStmt;
	private AccessPath<Field> resolvedAccPath;
	private Set<WrappedFact<Field, Fact, Stmt, Method>> incomingFacts = Sets.newHashSet();
	private boolean propagated = false;
	private Map<AccessPath<Field>, ControlFlowJoinResolver<Field, Fact, Stmt, Method>> nestedResolvers = Maps.newHashMap();
	private ControlFlowJoinResolver<Field, Fact, Stmt, Method> parent;

	public ControlFlowJoinResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt joinStmt) {
		this(analyzer, joinStmt, new AccessPath<Field>(), null);
	}
	
	private ControlFlowJoinResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Stmt joinStmt, AccessPath<Field> resolvedAccPath, ControlFlowJoinResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer);
		this.joinStmt = joinStmt;
		this.resolvedAccPath = resolvedAccPath;
		this.parent = parent;
	}

	public void addIncoming(final WrappedFact<Field, Fact, Stmt, Method> fact) {
		if(resolvedAccPath.isPrefixOf(fact.getAccessPath()) == PrefixTestResult.GUARANTEED_PREFIX) {
			log("Incoming Fact "+fact);
			if(!incomingFacts.add(fact))
				return;
			
			interest(analyzer, this);
			for(ControlFlowJoinResolver<Field, Fact, Stmt, Method> nestedResolver : nestedResolvers.values())
				nestedResolver.addIncoming(fact);
			
			if(!propagated) {
				propagated=true;
				analyzer.processFlowFromJoinStmt(new WrappedFactAtStatement<>(joinStmt, new WrappedFact<>(
						fact.getFact(), new AccessPath<Field>(), this)));
			}
		}
		else if(fact.getAccessPath().isPrefixOf(resolvedAccPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			Delta<Field> delta = fact.getAccessPath().getDeltaTo(resolvedAccPath);
			fact.getResolver().resolve(new DeltaConstraint<>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
				@Override
				public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, 
						Resolver<Field, Fact, Stmt, Method> resolver) {
					ControlFlowJoinResolver.this.interest(analyzer, ControlFlowJoinResolver.this);
				}

				@Override
				public void canBeResolvedEmpty() {
					ControlFlowJoinResolver.this.canBeResolvedEmpty();
				}
			});
		}
	}
	
	private boolean isLocked() {
		if(recursiveLock)
			return true;
		if(parent == null)
			return false;
		return parent.isLocked();
	}
	
	@Override
	public void resolve(Constraint<Field> constraint, final InterestCallback<Field, Fact, Stmt, Method> callback) {
		log("Resolve: "+constraint);
		if(!constraint.canBeAppliedTo(resolvedAccPath) || isLocked())
			return;
		
		AccessPath<Field> candidateAccPath = constraint.applyToAccessPath(resolvedAccPath, false);
		recursiveLock = true;
		ControlFlowJoinResolver<Field, Fact, Stmt, Method> nestedResolver = getOrCreateNestedResolver(candidateAccPath);
		if(!nestedResolver.resolvedAccPath.equals(constraint.applyToAccessPath(resolvedAccPath, false)))
			throw new AssertionError();
		
		nestedResolver.registerCallback(callback);
		recursiveLock = false;
	}
	
	private ControlFlowJoinResolver<Field, Fact, Stmt, Method> getOrCreateNestedResolver(AccessPath<Field> candidateAccPath) {
		if(resolvedAccPath.equals(candidateAccPath))
			return this;
		
		if(!nestedResolvers.containsKey(candidateAccPath)) {
			assert resolvedAccPath.getDeltaTo(candidateAccPath).accesses.length <= 1;
			
			final ControlFlowJoinResolver<Field, Fact, Stmt, Method> nestedResolver = new ControlFlowJoinResolver<>(analyzer, joinStmt, candidateAccPath, this);
			nestedResolver.propagated = true;
			nestedResolvers.put(candidateAccPath, nestedResolver);

			for(WrappedFact<Field, Fact, Stmt, Method> incFact: incomingFacts) {
				nestedResolver.addIncoming(incFact);
			}			
		}
		return nestedResolvers.get(candidateAccPath);
	}

	@Override
	protected void log(String message) {
		analyzer.log("Join Stmt "+toString()+": "+message);
	}

	@Override
	public String toString() {
		return "<"+resolvedAccPath+":"+joinStmt+">";
	}

	public AccessPath<Field> getResolvedAccessPath() {
		return resolvedAccPath;
	}

	public Stmt getJoinStmt() {
		return joinStmt;
	}
}
