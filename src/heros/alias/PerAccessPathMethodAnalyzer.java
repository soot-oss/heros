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
import heros.alias.FlowFunction.ConstrainedFact;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class PerAccessPathMethodAnalyzer<Field extends AccessPath.FieldRef<Field>, Fact, Stmt, Method> {

	protected static final Logger logger = LoggerFactory.getLogger(PerAccessPathMethodAnalyzer.class);
	private Fact sourceFact;
	private final AccessPath<Field> accessPath;
	private Map<WrappedFactAtStatement<Field,Fact, Stmt, Method>, WrappedFactAtStatement<Field,Fact, Stmt, Method>> reachableStatements = Maps.newHashMap();
	private List<WrappedFactAtStatement<Field, Fact, Stmt, Method>> summaries = Lists.newLinkedList();
	private Context<Field, Fact, Stmt, Method> context;
	private Method method;
	private Set<IncomingEdge<Field, Fact, Stmt, Method>> incomingEdges = Sets.newHashSet();
	private Map<AccessPath<Field>, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> nestedAnalyzers = Maps.newHashMap();
	private boolean bootstrapped = false;
	private CacheMap<FactAtStatement<Fact, Stmt>, ReturnSiteResolver<Field, Fact, Stmt, Method>> returnSiteResolvers = new CacheMap<FactAtStatement<Fact, Stmt>, ReturnSiteResolver<Field,Fact,Stmt,Method>>() {
		@Override
		protected ReturnSiteResolver<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			return new ReturnSiteResolver<>(PerAccessPathMethodAnalyzer.this, key.stmt);
		}
	};
	private CacheMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Field, Fact, Stmt, Method>> ctrFlowJoinResolvers = new CacheMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Field,Fact,Stmt,Method>>() {
		@Override
		protected ControlFlowJoinResolver<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			return new ControlFlowJoinResolver<>(PerAccessPathMethodAnalyzer.this, key.stmt);
		}
	};
	private CallEdgeResolver<Field, Fact, Stmt, Method> callEdgeResolver;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> parent;

	public PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context) {
		this(method, sourceFact, context, new AccessPath<Field>(), null);
	}
	
	private PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context, AccessPath<Field> accPath, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> parent) {
		if(method == null)
			throw new IllegalArgumentException("Method must be not null");
		this.parent = parent;
		this.method = method;
		this.sourceFact = sourceFact;
		this.accessPath = accPath;
		this.context = context;
		this.callEdgeResolver = isZeroSource() ? new ZeroCallEdgeResolver<>(this, context.zeroHandler) : new CallEdgeResolver<>(this);
		log("initialized");
	}
	
	WrappedFact<Field, Fact, Stmt, Method> wrappedSource() {
		return new WrappedFact<>(sourceFact, accessPath, callEdgeResolver);
	}
	
	public AccessPath<Field> getAccessPath() {
		return accessPath;
	}
	
	public void bootstrapAtMethodStartPoints() {
		if(bootstrapped)
			return;
		
		callEdgeResolver.interest(this, callEdgeResolver);
		bootstrapped = true;
		for(Stmt startPoint : context.icfg.getStartPointsOf(method)) {
			WrappedFactAtStatement<Field, Fact, Stmt, Method> target = new WrappedFactAtStatement<>(startPoint, wrappedSource());
			if(!reachableStatements.containsKey(target))
				scheduleEdgeTo(target);
		}
	}
	
	public void addInitialSeed(Stmt stmt) {
		scheduleEdgeTo(new WrappedFactAtStatement<>(stmt, wrappedSource()));
	}

	void scheduleEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if (reachableStatements.containsKey(factAtStmt)) {
			log("Merging "+factAtStmt);
			context.factHandler.merge(reachableStatements.get(factAtStmt).getFact().getFact(), factAtStmt.getFact().getFact());
		} else {
			log("Edge to "+factAtStmt);
			reachableStatements.put(factAtStmt, factAtStmt);
			context.scheduler.schedule(new Job(factAtStmt));
		}
	}

	void log(String message) {
		logger.trace("[{}; {}{}: "+message+"]", method, sourceFact, accessPath);
	}
	
	@Override
	public String toString() {
		return method+"; "+sourceFact+accessPath;
	}

	void processCall(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
		Collection<Method> calledMethods = context.icfg.getCalleesOfCallAt(factAtStmt.getStatement());
		for (Method calledMethod : calledMethods) {
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = context.flowProcessor.computeCallFlow(factAtStmt, calledMethod);
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				MethodAnalyzer<Field, Fact, Stmt, Method> analyzer = context.getAnalyzer(calledMethod);
				analyzer.addIncomingEdge(new IncomingEdge<Field, Fact, Stmt, Method>(this,
						factAtStmt, targetFact.getFact()));
			}
		}
		
		processCallToReturnEdge(factAtStmt);
	}

	void processExit(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		log("New Summary: "+factAtStmt);
		if(!summaries.add(factAtStmt))
			throw new AssertionError();

		
		for(IncomingEdge<Field, Fact, Stmt, Method> incEdge : incomingEdges) {
			applySummary(incEdge, factAtStmt);
		}

		if(context.followReturnsPastSeeds && isZeroSource()) {
			Collection<Stmt> callSites = context.icfg.getCallersOf(method);
			for(Stmt callSite : callSites) {
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(callSite);
				for(Stmt returnSite : returnSites) {
					Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = context.flowProcessor.computeUnbalancedReturnFlow(
							sourceFact, factAtStmt, method, returnSite, callSite);
					for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
						//TODO handle constraint
						context.getAnalyzer(context.icfg.getMethodOf(callSite)).addUnbalancedReturnFlow(new WrappedFactAtStatement<>(returnSite, targetFact.getFact()));
					}
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function will a null caller
			if(callSites.isEmpty()) {
				context.flowProcessor.computeUnbalancedReturnFlow(
						sourceFact, factAtStmt, method, null, null);
			}
		}
	}
	
	private void scheduleEdgeTo(Collection<Stmt> successors, WrappedFact<Field, Fact, Stmt, Method> fact) {
		for (Stmt stmt : successors) {
			scheduleEdgeTo(new WrappedFactAtStatement<>(stmt, fact));
		}
	}
	
	private void processCallToReturnEdge(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Stmt stmt = factAtStmt.getStatement();
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();		
		if(numberOfPredecessors > 1 || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getFact());
		}
		else {
			processNonJoiningCallToReturnFlow(factAtStmt);
		}
	}

	private void processNonJoiningCallToReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(factAtStmt.getStatement());
		for(Stmt returnSite : returnSites) {
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = context.flowProcessor.computeCallToReturnFlow(factAtStmt, returnSite);
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, targetFact.getFact()));
			}
		}
	}

	private void processNormalFlow(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
		Stmt stmt = factAtStmt.getStatement();
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();
		if((numberOfPredecessors > 1 && !context.icfg.isExitStmt(stmt)) || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getFact());
		}
		else {
			processNormalNonJoiningFlow(factAtStmt);
		}
	}

	void processFlowFromJoinStmt(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if(context.icfg.isCallStmt(factAtStmt.getStatement()))
			processNonJoiningCallToReturnFlow(factAtStmt);
		else
			processNormalNonJoiningFlow(factAtStmt);
	}
	
	private void processNormalNonJoiningFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		final List<Stmt> successors = context.icfg.getSuccsOf(factAtStmt.getStatement());
		Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = context.flowProcessor.computeNormalFlow(factAtStmt);
		for (final ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
			if(targetFact.getConstraint() == null)
				scheduleEdgeTo(successors, targetFact.getFact());
			else {
				targetFact.getFact().getResolver().resolve(targetFact.getConstraint(), new InterestCallback<Field, Fact, Stmt, Method>() {
					@Override
					public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
							Resolver<Field, Fact, Stmt, Method> resolver) {
						analyzer.scheduleEdgeTo(successors, new WrappedFact<>(targetFact.getFact().getFact(), targetFact.getFact().getAccessPath(), resolver));
					}

					@Override
					public void canBeResolvedEmpty() {
						callEdgeResolver.resolve(targetFact.getConstraint(), this);
					}
				});
			}
		}
	}
	
	PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getOrCreateNestedAnalyzer(AccessPath<Field> newAccPath) {
		if(newAccPath.equals(accessPath) || isZeroSource())
			return this;
		
		if(!nestedAnalyzers.containsKey(newAccPath)) {
			
			if(token)
				throw new AssertionError();
			
			assert accessPath.getDeltaTo(newAccPath).accesses.length <= 1;
			
			final PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> nestedAnalyzer = new PerAccessPathMethodAnalyzer<>(method, sourceFact, context, newAccPath, this);
			nestedAnalyzers.put(newAccPath, nestedAnalyzer);
			for(IncomingEdge<Field, Fact, Stmt, Method> incEdge : incomingEdges) {
				if(newAccPath.isPrefixOf(incEdge.getCalleeSourceFact().getAccessPath()) == PrefixTestResult.GUARANTEED_PREFIX)
					nestedAnalyzer.addIncomingEdge(incEdge);
				else if(incEdge.getCalleeSourceFact().getAccessPath().isPrefixOf(newAccPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX))
					incEdge.registerInterestCallback(nestedAnalyzer);
			}
		}
		return nestedAnalyzers.get(newAccPath);
	}
	
	boolean token;
	boolean recursiveLock;
	
	boolean isLocked() {
		if(recursiveLock)
			return true;
		if(parent == null)
			return false;
		return parent.isLocked();
	}
	
	public void addIncomingEdge(IncomingEdge<Field, Fact, Stmt, Method> incEdge) {
		if(accessPath.isPrefixOf(incEdge.getCalleeSourceFact().getAccessPath()) == PrefixTestResult.GUARANTEED_PREFIX) {
			log("Incoming Edge: "+incEdge);
			if(!incomingEdges.add(incEdge))
				return;
			
			callEdgeResolver.interest(this, callEdgeResolver);
			
			applySummaries(incEdge);
			token=true;
			for(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> nestedAnalyzer : nestedAnalyzers.values())
				nestedAnalyzer.addIncomingEdge(incEdge);
			token=false;
		}
		else if(incEdge.getCalleeSourceFact().getAccessPath().isPrefixOf(accessPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX)) {
			recursiveLock = true;
			incEdge.registerInterestCallback(this);
			recursiveLock = false;
		}
	}

	private void applySummary(IncomingEdge<Field, Fact, Stmt, Method> incEdge, WrappedFactAtStatement<Field, Fact, Stmt, Method> exitFact) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(incEdge.getCallSite());
		for(Stmt returnSite : returnSites) {
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = context.flowProcessor.computeReturnFlow(context.factHandler, exitFact, method, returnSite, incEdge);
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				scheduleReturnEdge(incEdge, targetFact.getFact(), returnSite);
			}
		}
	}

	void scheduleUnbalancedReturnEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact) {
		ReturnSiteResolver<Field,Fact,Stmt,Method> resolver = returnSiteResolvers.getOrCreate(fact.getAsFactAtStatement());
		resolver.addIncoming(new WrappedFact<>(fact.getFact().getFact(), fact.getFact().getAccessPath(), 
				fact.getFact().getResolver()), null, Delta.<Field>empty());
	}
	
	private void scheduleReturnEdge(IncomingEdge<Field, Fact, Stmt, Method> incEdge, WrappedFact<Field, Fact, Stmt, Method> fact, Stmt returnSite) {
		Delta<Field> delta = accessPath.getDeltaTo(incEdge.getCalleeSourceFact().getAccessPath());
		ReturnSiteResolver<Field, Fact, Stmt, Method> returnSiteResolver = incEdge.getCallerAnalyzer().returnSiteResolvers.getOrCreate(
				new FactAtStatement<Fact, Stmt>(fact.getFact(), returnSite));
		returnSiteResolver.addIncoming(fact, incEdge.getCalleeSourceFact().getResolver(), delta);
	}

	private void applySummaries(IncomingEdge<Field, Fact, Stmt, Method> incEdge) {
		for(WrappedFactAtStatement<Field, Fact, Stmt, Method> summary : summaries) {
			applySummary(incEdge, summary);
		}
	}
	
	public boolean isZeroSource() {
		return sourceFact.equals(context.zeroValue);
	}

	private class Job implements Runnable {

		private WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt;

		public Job(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			this.factAtStmt = factAtStmt;
		}

		@Override
		public void run() {
			if (context.icfg.isCallStmt(factAtStmt.getStatement())) {
				processCall(factAtStmt);
			} else {
				if (context.icfg.isExitStmt(factAtStmt.getStatement())) {
					processExit(factAtStmt);
				}
				if (!context.icfg.getSuccsOf(factAtStmt.getStatement()).isEmpty()) {
					processNormalFlow(factAtStmt);
				}
			}
		}
	}

	public CallEdgeResolver<Field, Fact, Stmt, Method> getCallEdgeResolver() {
		return callEdgeResolver;
	}
	
	public void debugInterest() {
		JsonDocument root = new JsonDocument();
		
		List<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> worklist = Lists.newLinkedList();
		worklist.add(this);
		Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> visited = Sets.newHashSet();
		
		while(!worklist.isEmpty()) {
			PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current = worklist.remove(0);
			if(!visited.add(current))
				continue;
			
			JsonDocument currentMethodDoc = root.doc(current.method.toString()+ "___"+current.sourceFact);
			JsonDocument currentDoc = currentMethodDoc.doc("accPath").doc("_"+current.accessPath.toString());
			
			for(IncomingEdge<Field, Fact, Stmt, Method> incEdge : current.incomingEdges) {
				currentDoc.doc("incoming").doc(incEdge.getCallerAnalyzer().method+"___"+incEdge.getCallerAnalyzer().sourceFact).doc("_"+incEdge.getCallerAnalyzer().accessPath.toString());
				worklist.add(incEdge.getCallerAnalyzer());
			}
		}
		
		try {
			FileWriter writer = new FileWriter("debug/incoming.json");
			StringBuilder builder = new StringBuilder();
			builder.append("var root=");
			root.write(builder, 0);
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void debugNestings() {
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current = this;
		while(current.parent != null)
			current = current.parent;
		
		JsonDocument root = new JsonDocument();
		debugNestings(current, root);
		
		try {
			FileWriter writer = new FileWriter("debug/nestings.json");
			StringBuilder builder = new StringBuilder();
			builder.append("var root=");
			root.write(builder, 0);
			writer.write(builder.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void debugNestings(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current, JsonDocument parentDoc) {
		JsonDocument currentDoc = parentDoc.doc(current.accessPath.toString());
		for(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> nestedAnalyzer : current.nestedAnalyzers.values()) {
			debugNestings(nestedAnalyzer, currentDoc);
		}
	}
}
