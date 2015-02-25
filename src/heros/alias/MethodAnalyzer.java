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


public class MethodAnalyzer<Field extends AccessPath.FieldRef<Field>,
							Fact, 
							Stmt, 
							Method> {

	private Method method;
	private CacheMap<Fact, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> perSourceAnalyzer = 
			new CacheMap<Fact, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>>() {
		@Override
		protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> createItem(Fact key) {
			return new PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>(method, key, context);
		}
	};
	private Context<Field, Fact, Stmt, Method> context;
	
	MethodAnalyzer(Method method, Context<Field, Fact, Stmt, Method> context) {
		this.method = method;
		this.context = context;
	}
	
	public void addIncomingEdge(IncomingEdge<Field, Fact, Stmt, Method> incEdge) {
		WrappedFact<Field, Fact, Stmt, Method> calleeSourceFact = incEdge.getCalleeSourceFact();
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer = perSourceAnalyzer.getOrCreate(calleeSourceFact.getFact());
		analyzer.bootstrapAtMethodStartPoints();
		analyzer.addIncomingEdge(incEdge);
	}

	public void addInitialSeed(Stmt startPoint, Fact val) {
		perSourceAnalyzer.getOrCreate(val).addInitialSeed(startPoint);
	}
	
	public void addUnbalancedReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> target) {
		perSourceAnalyzer.getOrCreate(context.zeroValue).scheduleUnbalancedReturnEdgeTo(target);
	}
}
