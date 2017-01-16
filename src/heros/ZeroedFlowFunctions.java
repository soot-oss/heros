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
package heros;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class ZeroedFlowFunctions<N, D, M> implements FlowFunctions<N, D, M> {

	protected final FlowFunctions<N, D, M> delegate;
	protected  final D zeroValue;

	public ZeroedFlowFunctions(FlowFunctions<N, D, M> delegate, D zeroValue) {
		this.delegate = delegate;
		this.zeroValue = zeroValue;
	}

	public FlowFunction<D> getNormalFlowFunction(D d1, N curr, N succ) {
		return new ZeroedFlowFunction(delegate.getNormalFlowFunction(d1, curr, succ));
	}

	public FlowFunction<D> getCallFlowFunction(D d1,N callStmt, M destinationMethod) {
		return new ZeroedFlowFunction(delegate.getCallFlowFunction(d1,callStmt, destinationMethod));
	}

	public FlowFunction<D> getReturnFlowFunction(D callerD1, D calleeD1, N callSite, D callerCallSiteFact, M calleeMethod, N exitStmt, N returnSite) {
		return new ZeroedFlowFunction(delegate.getReturnFlowFunction(callerD1,calleeD1,callSite, callerCallSiteFact, calleeMethod, exitStmt, returnSite));
	}

	public FlowFunction<D> getCallToReturnFlowFunction(D d1,N callSite, N returnSite, boolean hasCallees) {
		return new ZeroedFlowFunction(delegate.getCallToReturnFlowFunction(d1,callSite, returnSite,hasCallees));
	}
	
	protected class ZeroedFlowFunction implements FlowFunction<D> {

		protected FlowFunction<D> del;

		private ZeroedFlowFunction(FlowFunction<D> del) {
			this.del = del;
		}		
		
		@Override
		public Set<D> computeTargets(D source) {
			if(source==zeroValue) {
				HashSet<D> res = new LinkedHashSet<D>(del.computeTargets(source));
				res.add(zeroValue);
				return res;
			} else {
				return del.computeTargets(source);
			}
		}
		
	}
	

}
