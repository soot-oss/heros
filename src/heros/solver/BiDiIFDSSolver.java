/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.solver;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BiDiIFDSSolver<N, D, M, I extends InterproceduralCFG<N, M>> {

	private final IFDSTabulationProblem<N, AbstractionWithSourceStmt<N, D>, M, I> forwardProblem;
	private final IFDSTabulationProblem<N, AbstractionWithSourceStmt<N, D>, M, I> backwardProblem;
	private final CountingThreadPoolExecutor sharedExecutor;

	public BiDiIFDSSolver(IFDSTabulationProblem<N,D,M,I> forwardProblem, IFDSTabulationProblem<N,D,M,I> backwardProblem) {
		if(!forwardProblem.followReturnsPastSeeds() || !backwardProblem.followReturnsPastSeeds()) {
			throw new IllegalArgumentException("This solver is only meant for bottom-up problems, so followReturnsPastSeeds() should return true."); 
		}
		this.forwardProblem = new AugmentedTabulationProblem<N,D,M,I>(forwardProblem);
		this.backwardProblem = new AugmentedTabulationProblem<N,D,M,I>(backwardProblem);
		this.sharedExecutor = new CountingThreadPoolExecutor(1, forwardProblem.numThreads(), 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public void solve() {		
		IFDSSolver<N,AbstractionWithSourceStmt<N,D>,M,I> fwSolver = new SingleDirectionSolver(forwardProblem,"FW");

		IFDSSolver<N,AbstractionWithSourceStmt<N,D>,M,I> bwSolver = new SingleDirectionSolver(backwardProblem,"BW");
		
		bwSolver.submitInitialSeeds();
		fwSolver.solve();
	}
	
	private class SingleDirectionSolver extends IFDSSolver<N, AbstractionWithSourceStmt<N, D>, M, I> {
		private final String debugName;

		private SingleDirectionSolver(IFDSTabulationProblem<N, AbstractionWithSourceStmt<N, D>, M, I> ifdsProblem, String debugName) {
			super(ifdsProblem);
			this.debugName = debugName;
		}
		
		/* we share the same executor; this will cause the call to solve() above to block
		 * until both solvers have finished
		 */ 
		protected CountingThreadPoolExecutor getExecutor() {
			return sharedExecutor;
		}
		
		protected String getDebugName() {
			return debugName;
		}
	}

	public static class AbstractionWithSourceStmt<N,D> {

		protected final D abstraction;
		protected final N source;
		
		private AbstractionWithSourceStmt(D abstraction, N source) {
			this.abstraction = abstraction;
			this.source = source;
		}

		private D getAbstraction() {
			return abstraction;
		}
		
		private N getSourceStmt() {
			return source;
		}	
		
		@Override
		public String toString() {
			return "[["+abstraction+" from "+source+"]]";
		}
	}
	
	static class AugmentedTabulationProblem<N,D,M,I extends InterproceduralCFG<N, M>> implements IFDSTabulationProblem<N, BiDiIFDSSolver.AbstractionWithSourceStmt<N,D>,M,I> {

		private final IFDSTabulationProblem<N,D,M,I> delegate;
		private final AbstractionWithSourceStmt<N, D> ZERO;
		private final FlowFunctions<N, D, M> originalFunctions;
		
		public AugmentedTabulationProblem(IFDSTabulationProblem<N, D, M, I> delegate) {
			this.delegate = delegate;
			originalFunctions = this.delegate.flowFunctions();
			ZERO = new AbstractionWithSourceStmt<N, D>(delegate.zeroValue(), null);
		}

		@Override
		public FlowFunctions<N, AbstractionWithSourceStmt<N, D>, M> flowFunctions() {
			return new FlowFunctions<N, AbstractionWithSourceStmt<N, D>, M>() {

				@Override
				public FlowFunction<AbstractionWithSourceStmt<N, D>> getNormalFlowFunction(final N curr, final N succ) {
					return new FlowFunction<BiDiIFDSSolver.AbstractionWithSourceStmt<N,D>>() {
						@Override
						public Set<AbstractionWithSourceStmt<N, D>> computeTargets(AbstractionWithSourceStmt<N, D> source) {
							return copyOverSourceStmts(source, originalFunctions.getNormalFlowFunction(curr, succ));
						}
					};
				}

				@Override
				public FlowFunction<AbstractionWithSourceStmt<N, D>> getCallFlowFunction(final N callStmt, final M destinationMethod) {
					return new FlowFunction<BiDiIFDSSolver.AbstractionWithSourceStmt<N,D>>() {
						@Override
						public Set<AbstractionWithSourceStmt<N, D>> computeTargets(AbstractionWithSourceStmt<N, D> source) {
							return copyOverSourceStmts(source, originalFunctions.getCallFlowFunction(callStmt, destinationMethod));
						}
					};
				}

				@Override
				public FlowFunction<AbstractionWithSourceStmt<N, D>> getReturnFlowFunction(final N callSite, final M calleeMethod, final N exitStmt, final N returnSite) {
					return new FlowFunction<BiDiIFDSSolver.AbstractionWithSourceStmt<N,D>>() {
						@Override
						public Set<AbstractionWithSourceStmt<N, D>> computeTargets(AbstractionWithSourceStmt<N, D> source) {
							return copyOverSourceStmts(source, originalFunctions.getReturnFlowFunction(callSite, calleeMethod, exitStmt, returnSite));
						}
					};
				}

				@Override
				public FlowFunction<AbstractionWithSourceStmt<N, D>> getCallToReturnFlowFunction(final N callSite, final N returnSite) {
					return new FlowFunction<BiDiIFDSSolver.AbstractionWithSourceStmt<N,D>>() {
						@Override
						public Set<AbstractionWithSourceStmt<N, D>> computeTargets(AbstractionWithSourceStmt<N, D> source) {
							return copyOverSourceStmts(source, originalFunctions.getCallToReturnFlowFunction(callSite, returnSite));
						}
					};
				}
				
				private Set<AbstractionWithSourceStmt<N, D>> copyOverSourceStmts(AbstractionWithSourceStmt<N, D> source, FlowFunction<D> originalFunction) {
					D originalAbstraction = source.getAbstraction();
					Set<D> origTargets = originalFunction.computeTargets(originalAbstraction);

					//optimization
					if(origTargets.equals(Collections.singleton(originalAbstraction))) return Collections.singleton(source); 
					
					Set<AbstractionWithSourceStmt<N, D>> res = new HashSet<AbstractionWithSourceStmt<N,D>>();
					for(D d: origTargets) {
						res.add(new AbstractionWithSourceStmt<N,D>(d,source.getSourceStmt()));
					}
					return res;
				}
			};
		}
		
		//delegate methods follow

		public boolean followReturnsPastSeeds() {
			return delegate.followReturnsPastSeeds();
		}

		public boolean autoAddZero() {
			return delegate.autoAddZero();
		}

		public int numThreads() {
			return delegate.numThreads();
		}

		public boolean computeValues() {
			return delegate.computeValues();
		}

		public I interproceduralCFG() {
			return delegate.interproceduralCFG();
		}

		/* attaches the original seed statement to the abstraction
		 */
		public Map<N,Set<AbstractionWithSourceStmt<N, D>>> initialSeeds() {
			Map<N, Set<D>> originalSeeds = delegate.initialSeeds();
			Map<N,Set<AbstractionWithSourceStmt<N, D>>> res = new HashMap<N, Set<AbstractionWithSourceStmt<N,D>>>();
			for(Entry<N, Set<D>> entry: originalSeeds.entrySet()) {
				N stmt = entry.getKey();
				Set<D> seeds = entry.getValue();
				Set<AbstractionWithSourceStmt<N, D>> resSet = new HashSet<AbstractionWithSourceStmt<N,D>>();
				for (D d : seeds) {
					//attach source stmt to abstraction
					resSet.add(new AbstractionWithSourceStmt<N,D>(d, stmt));
				}
				res.put(stmt, resSet);
			}			
			return res;
		}

		public AbstractionWithSourceStmt<N, D> zeroValue() {
			return ZERO;
		}

	}
	
}
