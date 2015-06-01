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

import heros.fieldsens.FlowFunction.ConstrainedFact;
import heros.fieldsens.FlowFunction.ReadFieldConstraint;
import heros.fieldsens.FlowFunction.WriteFieldConstraint;

public class AccessPathHandler<Field, Fact, Stmt, Method> {

	private AccessPath<Field> accessPath;
	private Resolver<Field, Fact, Stmt, Method> resolver;

	public AccessPathHandler(AccessPath<Field> accessPath, Resolver<Field, Fact, Stmt, Method> resolver) {
		this.accessPath = accessPath;
		this.resolver = resolver;
	}

	public boolean canRead(Field field) {
		return accessPath.canRead(field);
	}
	
	public boolean mayCanRead(Field field) {
		return accessPath.canRead(field) || (accessPath.hasEmptyAccessPath() && !accessPath.isAccessInExclusions(field));
	}
	
	public boolean mayBeEmpty() {
		return accessPath.hasEmptyAccessPath();
	}

	public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
		return new ConstrainedFact<>(new WrappedFact<Field, Fact, Stmt, Method>(fact, accessPath, resolver));
	}
	
	public ConstrainedFact<Field, Fact, Stmt, Method> generateWithEmptyAccessPath(Fact fact, ZeroHandler<Field> zeroHandler) {
		return new ConstrainedFact<>(new WrappedFact<>(fact, new AccessPath<Field>(), new ZeroCallEdgeResolver<>(resolver.analyzer, zeroHandler)));
	}
	
	public ResultBuilder<Field, Fact, Stmt, Method> prepend(final Field field) {
		return new ResultBuilder<Field, Fact, Stmt, Method>() {
			@Override
			public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
				return new ConstrainedFact<>(new WrappedFact<>(fact, accessPath.prepend(field), resolver));
			}
		};
	}
	
	public ResultBuilder<Field, Fact, Stmt, Method> read(final Field field) {
		if(mayCanRead(field)) {
			return new ResultBuilder<Field, Fact, Stmt, Method>() {
				@Override
				public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
					if(canRead(field))
						return new ConstrainedFact<>(new WrappedFact<>(fact, accessPath.removeFirst(), resolver));
					else
						return new ConstrainedFact<>(new WrappedFact<>(fact, new AccessPath<Field>(), resolver), new ReadFieldConstraint<>(field));
				}
			};
		}
		else
			throw new IllegalArgumentException("Cannot read field "+field);
	}
	
	public ResultBuilder<Field, Fact, Stmt, Method> overwrite(final Field field) {
		if(mayBeEmpty())
			return new ResultBuilder<Field, Fact, Stmt, Method>() {
				@Override
				public ConstrainedFact<Field, Fact, Stmt, Method> generate(Fact fact) {
					if(accessPath.canRead(field)) {
						AccessPath<Field> tempAccPath = accessPath.removeFirst();
						if(tempAccPath.hasEmptyAccessPath())
							return new ConstrainedFact<>(new WrappedFact<>(fact, tempAccPath.appendExcludedFieldReference(field), resolver));
						else
							return new ConstrainedFact<>(new WrappedFact<>(fact, tempAccPath, resolver));
					} else if(accessPath.isAccessInExclusions(field))
						return new ConstrainedFact<>(new WrappedFact<>(fact, accessPath, resolver));
					else
						return new ConstrainedFact<>(new WrappedFact<>(fact, accessPath.appendExcludedFieldReference(field), resolver), new WriteFieldConstraint<>(field));
				}
			};
		else
			throw new IllegalArgumentException("Cannot write field "+field);
	}
	
	public static interface ResultBuilder<FieldRef, FactAbstraction, Stmt, Method> {
		public ConstrainedFact<FieldRef, FactAbstraction, Stmt, Method> generate(FactAbstraction fact);
	}

}
