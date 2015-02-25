/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.alias;

import heros.alias.TestHelper.Edge;
import heros.alias.TestHelper.ExpectedFlowFunction;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;


public abstract class EdgeBuilder {
	
	protected List<Edge> edges = Lists.newLinkedList();
	public Collection<Edge> edges() {
		if(edges.isEmpty()) {
			throw new IllegalStateException("Not a single edge created on EdgeBuilder: "+toString());
		}
		
		return edges;
	}

	public static class CallSiteBuilder extends EdgeBuilder {

		private TestStatement callSite;

		public CallSiteBuilder(TestStatement callSite) {
			this.callSite = callSite;
		}

		public CallSiteBuilder calls(String method, ExpectedFlowFunction...flows) {
			edges.add(new TestHelper.CallEdge(callSite, new TestMethod(method), flows));
			return this;
		}
		
		public CallSiteBuilder retSite(String returnSite, ExpectedFlowFunction...flows) {
			edges.add(new TestHelper.Call2ReturnEdge(callSite, new TestStatement(returnSite), flows));
			return this;
		}
	}
	
	public static class NormalStmtBuilder extends EdgeBuilder {

		private TestStatement stmt;
		private ExpectedFlowFunction[] flowFunctions;

		public NormalStmtBuilder(TestStatement stmt, ExpectedFlowFunction[] flowFunctions) {
			this.stmt = stmt;
			this.flowFunctions = flowFunctions;
		}

		public NormalStmtBuilder succ(String succ) {
			edges.add(new TestHelper.NormalEdge(stmt, new TestStatement(succ), flowFunctions));
			return this;
		}
	}
	
	public static class ExitStmtBuilder extends EdgeBuilder {

		private TestStatement exitStmt;

		public ExitStmtBuilder(TestStatement exitStmt) {
			this.exitStmt = exitStmt;
		}
		
		public ExitStmtBuilder expectArtificalFlow(ExpectedFlowFunction...flows) {
			edges.add(new TestHelper.ReturnEdge(null, exitStmt, null, flows));
			return this;
		}

		public ExitStmtBuilder returns(TestStatement callSite, TestStatement returnSite, ExpectedFlowFunction... flows) {
			edges.add(new TestHelper.ReturnEdge(callSite, exitStmt, returnSite, flows));
			return this;
		}
		
	}
}
