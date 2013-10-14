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

import heros.EdgeFunction;
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

/**
 * This is a special IFDS solver that solves the analysis problem inside out, i.e., from further down the call stack to
 * further up the call stack. This can be useful, for instance, for taint analysis problems that track flows in two directions.
 * 
 * The solver is instantiated with two analyses, one to be computed forward and one to be computed backward. Both analysis problems
 * must be unbalanced, i.e., must return <code>true</code> for {@link IFDSTabulationProblem#followReturnsPastSeeds()}.
 * The solver then executes both analyses in lockstep, i.e., when one of the analyses reaches an unbalanced return edge (signified
 * by a ZERO source value) then the solver pauses this analysis until the other analysis reaches the same unbalanced return (if ever).
 * The result is that the analyses will never diverge, i.e., will ultimately always only propagate into contexts in which both their
 * computed paths are realizable at the same time.
 *
 * @param <N> see {@link IFDSSolver}
 * @param <D> see {@link IFDSSolver}
 * @param <M> see {@link IFDSSolver}
 * @param <I> see {@link IFDSSolver}
 */
public class BiDiIFDSSolver<N, D, M, I extends InterproceduralCFG<N, M>> {

	private final IFDSTabulationProblem<N, AbstractionWithSourceStmt<N, D>, M, I> forwardProblem;
	private final IFDSTabulationProblem<N, AbstractionWithSourceStmt<N, D>, M, I> backwardProblem;
	private final CountingThreadPoolExecutor sharedExecutor;
	private SingleDirectionSolver fwSolver;
	private SingleDirectionSolver bwSolver;

	/**
	 * Instantiates a {@link BiDiIFDSSolver} with the associated forward and backward problem.
	 */
	public BiDiIFDSSolver(IFDSTabulationProblem<N,D,M,I> forwardProblem, IFDSTabulationProblem<N,D,M,I> backwardProblem) {
		if(!forwardProblem.followReturnsPastSeeds() || !backwardProblem.followReturnsPastSeeds()) {
			throw new IllegalArgumentException("This solver is only meant for bottom-up problems, so followReturnsPastSeeds() should return true."); 
		}
		this.forwardProblem = new AugmentedTabulationProblem<N,D,M,I>(forwardProblem);
		this.backwardProblem = new AugmentedTabulationProblem<N,D,M,I>(backwardProblem);
		this.sharedExecutor = new CountingThreadPoolExecutor(1, Math.max(1,forwardProblem.numThreads()), 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public void solve() {		
		fwSolver = new SingleDirectionSolver(forwardProblem, "FW");
		bwSolver = new SingleDirectionSolver(backwardProblem,"BW");
		fwSolver.otherSolver = bwSolver;
		bwSolver.otherSolver = fwSolver;
		
		//start the bw solver
		bwSolver.submitInitialSeeds();
		
		//start the fw solver and block until both solvers have completed
		//(note that they both share the same executor, see below)
		//note to self: the order of the two should not matter
		fwSolver.solve();
	}
	
	/**
	 * This is a modified IFDS solver that is capable of pausing and unpausing return-flow edges.
	 */
	private class SingleDirectionSolver extends IFDSSolver<N, AbstractionWithSourceStmt<N, D>, M, I> {
		private final String debugName;
		private SingleDirectionSolver otherSolver;
		private Set<N> leakedSources = new HashSet<N>();
		private Map<N,Set<PathEdge<N,AbstractionWithSourceStmt<N,D>>>> pausedPathEdges =
				new HashMap<N,Set<PathEdge<N,AbstractionWithSourceStmt<N,D>>>>();

		private SingleDirectionSolver(IFDSTabulationProblem<N, AbstractionWithSourceStmt<N, D>, M, I> ifdsProblem, String debugName) {
			super(ifdsProblem);
			this.debugName = debugName;
		}
		
		@Override
		protected void processExit(PathEdge<N,AbstractionWithSourceStmt<N,D>> edge) {
			//if an edge is originating from ZERO then to us this signifies an unbalanced return edge
			if(edge.factAtSource().equals(zeroValue)) {
				N sourceStmt = edge.factAtTarget().getSourceStmt();
				//we mark the fact that this solver would like to "leak" this edge to the caller
				leakedSources.add(sourceStmt);
				if(otherSolver.hasLeaked(sourceStmt)) {
					//if the other solver has leaked already then unpause its edges and continue
					otherSolver.unpausePathEdgesForSource(sourceStmt);
					super.processExit(edge);
				} else {
					//otherwise we pause this solver's edge and don't continue
					Set<PathEdge<N,AbstractionWithSourceStmt<N,D>>> pausedEdges = pausedPathEdges.get(sourceStmt);
					if(pausedEdges==null) {
						pausedEdges = new HashSet<PathEdge<N,AbstractionWithSourceStmt<N,D>>>();
						pausedPathEdges.put(sourceStmt,pausedEdges);
					}				
					pausedEdges.add(edge);
                    logger.debug(" ++ PAUSE {}: {}", debugName, edge);
				}
			} else {
				//the default case
				super.processExit(edge);
			}
		}
		
		protected void propagate(AbstractionWithSourceStmt<N,D> sourceVal, N target, AbstractionWithSourceStmt<N,D> targetVal, EdgeFunction<IFDSSolver.BinaryDomain> f, N relatedCallSite, boolean isUnbalancedReturn) {
			//the follwing branch will be taken only on an unbalanced return
			if(isUnbalancedReturn) {
				assert sourceVal.getSourceStmt()==null : "source value should have no statement attached";
				
				//attach target statement as new "source" statement to track
				targetVal = new AbstractionWithSourceStmt<N, D>(targetVal.getAbstraction(), relatedCallSite);
				
				super.propagate(sourceVal, target, targetVal, f, relatedCallSite, isUnbalancedReturn);
			} else { 
				super.propagate(sourceVal, target, targetVal, f, relatedCallSite, isUnbalancedReturn);
			}
		}
		
		/**
		 * Returns <code>true</code> if this solver has tried to leak an edge originating from the given source
		 * to its caller.
		 */
		private boolean hasLeaked(N sourceStmt) {
			return leakedSources.contains(sourceStmt);
		}
		
		/**
		 * Unpauses all edges associated with the given source statement.
		 */
		private void unpausePathEdgesForSource(N sourceStmt) {
			Set<PathEdge<N, AbstractionWithSourceStmt<N, D>>> pausedEdges = pausedPathEdges.get(sourceStmt);
			if(pausedEdges!=null) {
				for(PathEdge<N, AbstractionWithSourceStmt<N, D>> pausedEdge: pausedEdges) {
					logger.debug("-- UNPAUSE {}: {}",debugName, pausedEdge);
					super.processExit(pausedEdge);
				}
				pausedPathEdges.remove(sourceStmt);
			}
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

	/**
	 * This is an augmented abstraction propagated by the {@link SingleDirectionSolver}. It associates with the
	 * abstraction the source statement from which this fact originated. 
	 */
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
			if(source!=null)
				return ""+abstraction+"-@-"+source+"";
			else
				return abstraction.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AbstractionWithSourceStmt other = (AbstractionWithSourceStmt) obj;
			if (abstraction == null) {
				if (other.abstraction != null)
					return false;
			} else if (!abstraction.equals(other.abstraction))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}
	}
	
	/**
	 * This tabulation problem simply propagates augmented abstractions where the normal problem would propagate normal abstractions.
	 */
	private static class AugmentedTabulationProblem<N,D,M,I extends InterproceduralCFG<N, M>>
		implements IFDSTabulationProblem<N, BiDiIFDSSolver.AbstractionWithSourceStmt<N,D>,M,I> {

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
	
	public Set<D> fwIFDSResultAt(N stmt) {
		return extractResults(fwSolver.ifdsResultsAt(stmt));
	}

	
	public Set<D> bwIFDSResultAt(N stmt) {
		return extractResults(bwSolver.ifdsResultsAt(stmt));
	}

	private Set<D> extractResults(Set<AbstractionWithSourceStmt<N, D>> annotatedResults) {
		Set<D> res = new HashSet<D>();		
		for (AbstractionWithSourceStmt<N, D> abstractionWithSourceStmt : annotatedResults) {
			res.add(abstractionWithSourceStmt.getAbstraction());
		}
		return res;
	}
	
}
