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

import heros.alias.FlowFunction.Constraint;


class CallEdgeResolver<Field, Fact, Stmt, Method> extends Resolver<Field, Fact, Stmt, Method>  {

	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
		super(analyzer);
	}

	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {		
		log("Resolve: "+constraint);
		if(constraint.canBeAppliedTo(analyzer.getAccessPath()) && !analyzer.isLocked() && !doesContain(constraint)) {
			AccessPath<Field> newAccPath = constraint.applyToAccessPath(analyzer.getAccessPath());
			PerAccessPathMethodAnalyzer<Field,Fact,Stmt,Method> nestedAnalyzer = analyzer.getOrCreateNestedAnalyzer(newAccPath);
			nestedAnalyzer.getCallEdgeResolver().registerCallback(callback);
		}
	}
	
	//FIXME: this is a dirty hack (and unsound?!)
	private boolean doesContain(Constraint<Field> constraint) {
		AccessPath<Field> accPath = constraint.applyToAccessPath(new AccessPath<Field>());
		return analyzer.getAccessPath().contains(accPath);
	}

	@Override
	public String toString() {
		return "";
	}
	
	@Override
	protected void log(String message) {
		analyzer.log(message);
	}


}