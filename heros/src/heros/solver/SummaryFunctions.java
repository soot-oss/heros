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
import heros.SynchronizedBy;
import heros.ThreadSafe;

import java.util.Collections;
import java.util.Map;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


/**
 * A data structure to record summary functions in an indexed fashion, for fast retrieval.
 */
@ThreadSafe
public class SummaryFunctions<N,D,V> {
	
	@SynchronizedBy("consistent lock on this")
	protected Table<N,D,Table<N,D,EdgeFunction<V>>> table = HashBasedTable.create();
	
	/**
	 * Inserts a summary function.
	 * @param callSite The call site with which this function is associated.
	 * @param sourceVal The source value at the call site. 
	 * @param retSite The return site (in the caller) with which this function is associated.
	 * @param targetVal The target value at the return site.
	 * @param function The edge function used to compute V-type values from the source node to the target node.  
	 */
	public synchronized void insertFunction(N callSite,D sourceVal, N retSite, D targetVal, EdgeFunction<V> function) {
		assert callSite!=null;
		assert sourceVal!=null;
		assert retSite!=null;
		assert targetVal!=null;
		assert function!=null;
		
		Table<N, D, EdgeFunction<V>> targetAndTargetValToFunction = table.get(callSite,sourceVal);
		if(targetAndTargetValToFunction==null) {
			targetAndTargetValToFunction = HashBasedTable.create();
			table.put(callSite,sourceVal,targetAndTargetValToFunction);
		}
		targetAndTargetValToFunction.put(retSite, targetVal, function);
	}

	/**
	 * Retrieves all summary functions for a given call site, source value and
	 * return site (in the caller).
	 * The result contains a mapping from target value to associated edge function.
	 */
	public synchronized Map<D,EdgeFunction<V>> summariesFor(N callSite, D sourceVal, N returnSite) {
		assert callSite!=null;
		assert sourceVal!=null;
		assert returnSite!=null;

		Table<N, D, EdgeFunction<V>> res = table.get(callSite,sourceVal);
		if(res==null) return Collections.emptyMap();
		else {
			return res.row(returnSite);
		}
	}
}
