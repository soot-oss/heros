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
package heros.fieldsens;

import heros.InterproceduralCFG;
import heros.fieldsens.FlowFunction.Constraint;
import heros.fieldsens.structs.WrappedFactAtStatement;

public interface Debugger<Field, Fact, Stmt, Method> {

	public void setICFG(InterproceduralCFG<Stmt, Method> icfg);
	public void initialSeed(Stmt stmt);
	public void edgeTo(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt);
	public void newResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver);
	public void newJob(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt);
	public void jobStarted(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt);
	public void jobFinished(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt);
	public void askedToResolve(Resolver<Field, Fact, Stmt, Method> resolver, Constraint<Field> constraint);
	
	public static class NullDebugger <Field, Fact, Stmt, Method> implements Debugger<Field, Fact, Stmt, Method> {

		@Override
		public void setICFG(InterproceduralCFG<Stmt, Method> icfg) {
			
		}

		@Override
		public void initialSeed(Stmt stmt) {
			
		}

		@Override
		public void edgeTo(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
				WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			
		}

		@Override
		public void newResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
			
		}

		@Override
		public void newJob(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
				WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			
		}

		@Override
		public void jobStarted(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
				WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			
		}

		@Override
		public void jobFinished(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
				WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			
		}

		@Override
		public void askedToResolve(Resolver<Field, Fact, Stmt, Method> resolver, Constraint<Field> constraint) {
			
		}
		
	}
}