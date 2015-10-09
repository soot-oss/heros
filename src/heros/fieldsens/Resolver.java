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

import heros.fieldsens.FlowFunction.Constraint;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Resolver<Field, Fact, Stmt, Method> {

	private Set<Resolver<Field, Fact, Stmt, Method>> interest = Sets.newHashSet();
	private List<InterestCallback<Field, Fact, Stmt, Method>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;
	private boolean canBeResolvedEmpty = false;
	
	public Resolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
		this.analyzer = analyzer;
	}

	public abstract void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback);
	
	public void interest(Resolver<Field, Fact, Stmt, Method> resolver) {
		if(!interest.add(resolver))
			return;

		log("Interest given by: "+resolver);
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(analyzer, resolver);
		}
	}
	
	protected void canBeResolvedEmpty() {
		if(canBeResolvedEmpty)
			return;
		
		canBeResolvedEmpty = true;
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.canBeResolvedEmpty();
		}
	}

	public boolean isInterestGiven() {
		return !interest.isEmpty();
	}

	protected void registerCallback(InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(!interest.isEmpty()) {
			for(Resolver<Field, Fact, Stmt, Method> resolver : Lists.newLinkedList(interest))
				callback.interest(analyzer, resolver);
		}
		log("Callback registered");
		interestCallbacks.add(callback);

		if(canBeResolvedEmpty)
			callback.canBeResolvedEmpty();
	}
	
	protected abstract void log(String message);
	
}
