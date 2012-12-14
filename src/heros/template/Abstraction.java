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
package heros.template;

import heros.FlowFunction;

import java.util.Set;

/**
 * A default abstraction object that can be used to double-dispatch on the abstraction type.
 *
 * @param <N> The type of nodes in the interprocedural control-flow graph. Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods. Typically {@link SootMethod}.
 */
public abstract class Abstraction<N,D,M> {
	
	abstract Set<D> normalFlowTargets(D source, N curr, N succ);
	
	abstract Set<D> callFlowTargets(D source, N callStmt, M destinationMethod);
	
	abstract Set<D> returnFlowTargets(D source, N callSite, M calleeMethod, N exitStmt, N returnSite);
	
	abstract Set<D> callToReturnFlowTargets(D source, N callSite, N returnSite);

	public FlowFunction<D> normalFlowFunction(final N curr, final N succ) {
		return new FlowFunction<D>() {
			public Set<D> computeTargets(D source) {
				return normalFlowTargets(source, curr, succ);
			}
		};
	}
	
	public FlowFunction<D> callFlowFunction(final N callStmt, final M destinationMethod) {
		return new FlowFunction<D>() {
			public Set<D> computeTargets(D source) {
				return callFlowTargets(source, callStmt, destinationMethod);
			}
		};
	}
	
	public FlowFunction<D> returnFlowFunction(final N callSite, final M calleeMethod, final N exitStmt, final N returnSite) {
		return new FlowFunction<D>() {
			public Set<D> computeTargets(D source) {
				return returnFlowTargets(source, callSite, calleeMethod, exitStmt, returnSite);
			}
		};
	}
	
	public FlowFunction<D> callToReturnFlowFunction(final N callSite, final N returnSite) {
		return new FlowFunction<D>() {
			public Set<D> computeTargets(D source) {
				return callToReturnFlowTargets(source, callSite, returnSite);
			}
		};
	}		
	
}
