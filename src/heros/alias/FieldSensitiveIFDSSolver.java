/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch, Johannes Späth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch, Johannes Späth - initial API and implementation
 ******************************************************************************/
package heros.alias;

import heros.InterproceduralCFG;

import java.util.Map.Entry;
import java.util.Set;

public class FieldSensitiveIFDSSolver<N, FieldRef extends AccessPath.FieldRef<FieldRef>, D, M, I extends InterproceduralCFG<N, M>> {

	private Scheduler scheduler = new Scheduler();
	private FlowFunctionProcessor<D, N, M, FieldRef> flowProcessor;
	
	private CacheMap<M, MethodAnalyzer<FieldRef, D, N, M>> methodAnalyzers = new CacheMap<M, MethodAnalyzer<FieldRef, D,N, M>>() {
		@Override
		protected MethodAnalyzer<FieldRef, D, N, M> createItem(M key) {
			return new MethodAnalyzer<>(key, context);
		}
	};

	private IFDSTabulationProblem<N, FieldRef, D, M, I> tabulationProblem;
	private Context<FieldRef, D, N,M> context;
	private Debugger<FieldRef, D, N, M, I> debugger;

	public FieldSensitiveIFDSSolver(IFDSTabulationProblem<N,FieldRef,D,M,I> tabulationProblem, FactMergeHandler<D> factHandler, Debugger<FieldRef, D, N, M, I> debugger) {
		this.tabulationProblem = tabulationProblem;
		this.debugger = debugger == null ? new Debugger.NullDebugger<FieldRef, D, N, M, I>() : debugger;
		this.debugger.setICFG(tabulationProblem.interproceduralCFG());
		flowProcessor = new FlowFunctionProcessor<>(tabulationProblem.flowFunctions());
		context = new Context<FieldRef, D, N, M>(tabulationProblem.interproceduralCFG(), flowProcessor, scheduler, tabulationProblem.zeroValue(), 
				tabulationProblem.followReturnsPastSeeds(), factHandler, tabulationProblem.zeroHandler()) {
			@Override
			public MethodAnalyzer<FieldRef, D, N, M> getAnalyzer(M method) {
				if(method == null)
					throw new IllegalArgumentException("Method must be not null");
				return methodAnalyzers.getOrCreate(method);
			}
		};
	}

	/**
	 * Runs the solver on the configured problem. This can take some time.
	 */
	public void solve() {		
		submitInitialSeeds();
		scheduler.runAndAwaitCompletion();
	}

	/**
	 * Schedules the processing of initial seeds, initiating the analysis.
	 * Clients should only call this methods if performing synchronization on
	 * their own. Normally, {@link #solve()} should be called instead.
	 */
	protected void submitInitialSeeds() {
		for(Entry<N, Set<D>> seed: tabulationProblem.initialSeeds().entrySet()) {
			N startPoint = seed.getKey();
			MethodAnalyzer<FieldRef, D,N,M> analyzer = methodAnalyzers.getOrCreate(tabulationProblem.interproceduralCFG().getMethodOf(startPoint));
			for(D val: seed.getValue()) {
				analyzer.addInitialSeed(startPoint, val);
				debugger.initialSeed(startPoint);
			}
		}
	}
}
