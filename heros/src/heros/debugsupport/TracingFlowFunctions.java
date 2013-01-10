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
package heros.debugsupport;

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.solver.IDESolver;

import java.util.Collections;
import java.util.Set;

public class TracingFlowFunctions<N, D, M, I extends InterproceduralCFG<N,M>> implements FlowFunctions<N, D, M> {

	protected final FlowFunctions<N, D, M> delegate;
	protected final IDESolver<N, D, M, ?, I> solver;
	
	public TracingFlowFunctions(FlowFunctions<N, D, M> delegate, IDESolver<N, D, M, ?, I> solver) {
		this.delegate = delegate;
		this.solver = solver;
	}

	public FlowFunction<D> getNormalFlowFunction(N curr, N succ) {
		return new TracingFlowFunction(delegate.getNormalFlowFunction(curr, succ), curr, Collections.singleton(succ));
	}

	public FlowFunction<D> getCallFlowFunction(N callStmt, M destinationMethod) {
		return new TracingFlowFunction(delegate.getCallFlowFunction(callStmt, destinationMethod),callStmt,solver.getICFG().getStartPointsOf(destinationMethod));
	}

	public FlowFunction<D> getReturnFlowFunction(N callSite, M calleeMethod,N exitStmt, N returnSite) {
		return new TracingFlowFunction(delegate.getReturnFlowFunction(callSite, calleeMethod, exitStmt, returnSite),exitStmt,Collections.singleton(returnSite));
	}

	public FlowFunction<D> getCallToReturnFlowFunction(N callSite, N returnSite) {
		return new TracingFlowFunction(delegate.getCallToReturnFlowFunction(callSite, returnSite),callSite,Collections.singleton(returnSite));
	}
	
	class TracingFlowFunction implements FlowFunction<D> {
		
		protected final FlowFunction<D> del;
		protected final M fromMethod;
		protected final N from;
		protected final Set<N> to;
		
		public TracingFlowFunction(FlowFunction<D> del, N from, Set<N> to) {
			this.del = del;
			this.from = from;
			this.to = to;
			fromMethod = solver.getICFG().getMethodOf(from);
		}

		public Set<D> computeTargets(D source) {			
			Set<D> res = del.computeTargets(source);
			for(D r: res) {
				for(N t: to) {
					solver.getDebugListener().newFlow(fromMethod, from, source, t, r);
				}
			}
			return res;
		}		
	}
	
	
	
}
