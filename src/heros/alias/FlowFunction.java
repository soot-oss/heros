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
package heros.alias;

import heros.alias.FieldReference.SpecificFieldReference;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A flow function computes which of the finitely many D-type values are reachable
 * from the current source values. Typically there will be one such function
 * associated with every possible control flow. 
 * 
 * <b>NOTE:</b> To be able to produce <b>deterministic benchmarking results</b>, we have found that
 * it helps to return {@link LinkedHashSet}s from {@link #computeTargets(Object)}. This is
 * because the duration of IDE's fixed point iteration may depend on the iteration order.
 * Within the solver, we have tried to fix this order as much as possible, but the
 * order, in general, does also depend on the order in which the result set
 * of {@link #computeTargets(Object)} is traversed.
 * 
 * <b>NOTE:</b> Methods defined on this type may be called simultaneously by different threads.
 * Hence, classes implementing this interface should synchronize accesses to
 * any mutable shared state.
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public interface FlowFunction<D extends FieldSensitiveFact<?, D>> {

	/**
	 * Returns the target values reachable from the source.
	 */
	Set<AnnotatedFact<D>> computeTargets(D source);
	
	public static class AnnotatedFact<D extends FieldSensitiveFact<?, D>> {
		
		private D fact;
		private FieldReference readField;
		private FieldReference writtenField;
		
		//TODO: Refactor API to make things more intuitive
		/**
		 * 
		 * @param fact
		 * @param readField Giving a field reference here means the base value of a field access was tainted, i.e., we have to concretize the source value
		 * @param writtenField
		 */
		public AnnotatedFact(D fact, FieldReference readField, FieldReference writtenField) {
			this.fact = fact;
			this.readField = readField;
			this.writtenField = writtenField;
		}
		
		public D getFact() {
			return fact;
		}
		
		public FieldReference getReadField() {
			return readField;
		}
		
		public FieldReference getWrittenField() {
			return writtenField;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fact == null) ? 0 : fact.hashCode());
			result = prime * result + ((readField == null) ? 0 : readField.hashCode());
			result = prime * result + ((writtenField == null) ? 0 : writtenField.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AnnotatedFact))
				return false;
			AnnotatedFact other = (AnnotatedFact) obj;
			if (fact == null) {
				if (other.fact != null)
					return false;
			} else if (!fact.equals(other.fact))
				return false;
			if (readField == null) {
				if (other.readField != null)
					return false;
			} else if (!readField.equals(other.readField))
				return false;
			if (writtenField == null) {
				if (other.writtenField != null)
					return false;
			} else if (!writtenField.equals(other.writtenField))
				return false;
			return true;
		}
	}
}
