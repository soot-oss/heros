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

import heros.InterproceduralCFG;

public abstract class Context<FieldRef, FactAbstraction, Statement, Method> {

	public final InterproceduralCFG<Statement, Method> icfg;
	public final FlowFunctionProcessor<FactAbstraction, Statement, Method, FieldRef> flowProcessor;
	public final Scheduler scheduler;
	public final FactAbstraction zeroValue;
	public final boolean followReturnsPastSeeds;
	public final FactMergeHandler<FactAbstraction> factHandler;
	public final ZeroHandler<FieldRef> zeroHandler;
	
	Context(InterproceduralCFG<Statement, Method> icfg, FlowFunctionProcessor<FactAbstraction, Statement, Method, FieldRef> flowProcessor, 
			Scheduler scheduler, FactAbstraction zeroValue, boolean followReturnsPastSeeds, FactMergeHandler<FactAbstraction> factHandler, ZeroHandler<FieldRef> zeroHandler) {
		this.icfg = icfg;
		this.flowProcessor = flowProcessor;
		this.scheduler = scheduler;
		this.zeroValue = zeroValue;
		this.followReturnsPastSeeds = followReturnsPastSeeds;
		this.factHandler = factHandler;
		this.zeroHandler = zeroHandler;
	}
	
	public abstract MethodAnalyzer<FieldRef, FactAbstraction, Statement, Method> getAnalyzer(Method method);
}
