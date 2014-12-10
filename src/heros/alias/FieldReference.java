/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch, Johannes Späth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch, Johannes Späth - initial API and implementation
 ******************************************************************************/
package heros.alias;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public interface FieldReference {

	public static class Any implements FieldReference {
		Set<String> excludedFieldNames = Sets.newHashSet();
		
		public Any(String...excludedFieldNames) {
			for (int i = 0; i < excludedFieldNames.length; i++) {
				this.excludedFieldNames.add(excludedFieldNames[i]);
			}
		}
		
		@Override
		public String toString() {
			if(excludedFieldNames.size() == 0)
				return "";
			else if (excludedFieldNames.size() == 1)
				return "^" + excludedFieldNames.iterator().next();
			else
				return "^{" + Joiner.on(",").join(excludedFieldNames) +"}";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((excludedFieldNames == null) ? 0 : excludedFieldNames
							.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Any other = (Any) obj;
			if (excludedFieldNames == null) {
				if (other.excludedFieldNames != null)
					return false;
			} else if (!excludedFieldNames.equals(other.excludedFieldNames))
				return false;
			return true;
		}
	}
	
	public static class SpecificFieldReference implements FieldReference {
		private String fieldName;

		public SpecificFieldReference(String fieldName) {
			this.fieldName = fieldName;
		}

		@Override
		public String toString() {
			return fieldName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fieldName == null) ? 0 : fieldName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SpecificFieldReference other = (SpecificFieldReference) obj;
			if (fieldName == null) {
				if (other.fieldName != null)
					return false;
			} else if (!fieldName.equals(other.fieldName))
				return false;
			return true;
		}
	}
}
