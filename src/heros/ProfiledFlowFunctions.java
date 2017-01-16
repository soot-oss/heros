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

/**
 * A wrapper that can be used to profile flow functions.
 */
public class ProfiledFlowFunctions<N, D, M> implements FlowFunctions<N, D, M> {

	protected final FlowFunctions<N, D, M> delegate;
	
	public long durationNormal, durationCall, durationReturn, durationCallReturn;

	public ProfiledFlowFunctions(FlowFunctions<N, D, M> delegate) {
		this.delegate = delegate;
	}

	public FlowFunction<D> getNormalFlowFunction(D sourceFact, N curr, N succ) {
		long before = System.currentTimeMillis();
		FlowFunction<D> ret = delegate.getNormalFlowFunction(sourceFact, curr, succ);
		long duration = System.currentTimeMillis() - before;
		durationNormal += duration;
		return ret;
	}

	public FlowFunction<D> getCallFlowFunction(D sourceFact, N callStmt, M destinationMethod) {
		long before = System.currentTimeMillis();
		FlowFunction<D> res = delegate.getCallFlowFunction(sourceFact, callStmt, destinationMethod);
		long duration = System.currentTimeMillis() - before;
		durationCall += duration;
		return res;
	}

	public FlowFunction<D> getReturnFlowFunction(D callerD1,D calleeD1, N callSite,  D callerCallSiteFact, M calleeMethod, N exitStmt, N returnSite) {
		long before = System.currentTimeMillis();
		FlowFunction<D> res = delegate.getReturnFlowFunction(callerD1,calleeD1, callSite, callerCallSiteFact, calleeMethod, exitStmt, returnSite);
		long duration = System.currentTimeMillis() - before;
		durationReturn += duration;
		return res;
	}

	public FlowFunction<D> getCallToReturnFlowFunction(D sourceFact, N callSite, N returnSite, boolean hasCallee) {
		long before = System.currentTimeMillis();
		FlowFunction<D> res = delegate.getCallToReturnFlowFunction(sourceFact, callSite, returnSite, hasCallee);
		long duration = System.currentTimeMillis() - before;
		durationCallReturn += duration;
		return res;
	}
	
}
