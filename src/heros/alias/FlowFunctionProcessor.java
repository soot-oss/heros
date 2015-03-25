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

import heros.alias.FlowFunction.ConstrainedFact;

import java.util.Set;

import com.google.common.collect.Sets;

public class FlowFunctionProcessor<Fact, Stmt, Method, Field> {

	private FlowFunctions<Stmt, Field, Fact, Method> flowFunctions;

	public FlowFunctionProcessor(FlowFunctions<Stmt, Field, Fact, Method> flowFunctions) {
		this.flowFunctions = flowFunctions;
	}
	
	public Set<ConstrainedFact<Field, Fact, Stmt, Method>> computeNormalFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> source) {
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = flowFunctions.getNormalFlowFunction(source.getStatement());
		return flowFunction.computeTargets(source.getFact().getFact(), new AccessPathHandler<>(source.getFact().getAccessPath(), source.getFact().getResolver()));
	}
	
	public Set<ConstrainedFact<Field, Fact, Stmt, Method>> computeCallFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> source, Method calledMethod) {
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = flowFunctions.getCallFlowFunction(source.getStatement(), calledMethod);
		return flowFunction.computeTargets(source.getFact().getFact(), new AccessPathHandler<>(source.getFact().getAccessPath(), source.getFact().getResolver()));
	}

	public Set<ConstrainedFact<Field, Fact, Stmt, Method>> computeCallToReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> source, Stmt returnSite) {
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = flowFunctions.getCallToReturnFlowFunction(source.getStatement(), returnSite);
		return flowFunction.computeTargets(source.getFact().getFact(), new AccessPathHandler<>(source.getFact().getAccessPath(), source.getFact().getResolver()));
	}

	public Set<ConstrainedFact<Field, Fact, Stmt, Method>> computeReturnFlow(FactMergeHandler<Fact> factHandler, WrappedFactAtStatement<Field, Fact, Stmt, Method> source, Method calleeMethod, Stmt returnSite,
			IncomingEdge<Field, Fact, Stmt, Method> incEdge) {
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = flowFunctions.getReturnFlowFunction(incEdge.getCallSite(), calleeMethod, source.getStatement(), returnSite);
		Set<ConstrainedFact<Field, Fact, Stmt, Method>> targets = flowFunction.computeTargets(source.getFact().getFact(), new AccessPathHandler<>(source.getFact().getAccessPath(), source.getFact().getResolver()));
		
		for (ConstrainedFact<Field, Fact, Stmt, Method> constrainedFact : targets) {
			factHandler.restoreCallingContext(constrainedFact.getFact().getFact(), incEdge.getCallerCallSiteFact().getFact());
		}
		return targets;
	}

	public Set<ConstrainedFact<Field, Fact, Stmt, Method>> computeUnbalancedReturnFlow(Fact zero, WrappedFactAtStatement<Field, Fact, Stmt, Method> source, 
			Method calleeMethod, Stmt returnSite, Stmt callSite) {
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = flowFunctions.getReturnFlowFunction(callSite, calleeMethod, source.getStatement(), returnSite);
		return flowFunction.computeTargets(source.getFact().getFact(), new AccessPathHandler<>(source.getFact().getAccessPath(), source.getFact().getResolver()));
	}
}
