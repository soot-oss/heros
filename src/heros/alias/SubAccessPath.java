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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public interface SubAccessPath<FieldRef extends AccessPath.FieldRef<FieldRef>> {
	
	boolean contains(FieldRef field);
	
	boolean shouldBeMerged(FieldRef field);

	boolean shouldBeMerged(SubAccessPath<FieldRef> accPath);

	boolean intersects(SubAccessPath<FieldRef> accPath);

	Collection<? extends FieldRef> elements();
	
	SetOfPossibleFieldAccesses<FieldRef> merge(SubAccessPath<FieldRef>... fields);

	
	public static class SpecificFieldAccess<FieldRef extends AccessPath.FieldRef<FieldRef>> implements SubAccessPath<FieldRef> {
		private final FieldRef field;
		
		public SpecificFieldAccess(FieldRef field) {
			this.field = field;
		}

		@Override
		public boolean contains(FieldRef field) {
			return this.field.equals(field);
		}

		@Override
		public Collection<? extends FieldRef> elements() {
			return Sets.newHashSet(field);
		}

		@Override
		public SetOfPossibleFieldAccesses<FieldRef> merge(SubAccessPath<FieldRef>... fields) {
			HashSet<FieldRef> set = Sets.newHashSet();
			for(SubAccessPath<FieldRef> f : fields) {
				set.addAll(f.elements());
			}
			return new SetOfPossibleFieldAccesses<>(set);
		}
		
		@Override
		public String toString() {
			return field.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((field == null) ? 0 : field.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SpecificFieldAccess))
				return false;
			SpecificFieldAccess other = (SpecificFieldAccess) obj;
			if (field == null) {
				if (other.field != null)
					return false;
			} else if (!field.equals(other.field))
				return false;
			return true;
		}

		@Override
		public boolean intersects(SubAccessPath<FieldRef> accPath) {
			return accPath.contains(field);
		}

		@Override
		public boolean shouldBeMerged(SubAccessPath<FieldRef> accPath) {
			return accPath.shouldBeMerged(field);
		}

		@Override
		public boolean shouldBeMerged(FieldRef field) {
			return this.field.shouldBeMergedWith(field);
		}
	}
	
	public static class SetOfPossibleFieldAccesses<FieldRef extends AccessPath.FieldRef<FieldRef>> implements SubAccessPath<FieldRef> {
		
		private final Set<FieldRef> set;
		
		public SetOfPossibleFieldAccesses() {
			set = Sets.newHashSet();
		}

		public SetOfPossibleFieldAccesses(Set<FieldRef> set) {
			this.set = set;
		}

		@Override
		public boolean contains(FieldRef field) {
			return set.contains(field);
		}
		
		@Override
		public Collection<? extends FieldRef> elements() {
			return set;
		}

		@Override
		public SetOfPossibleFieldAccesses<FieldRef> merge(SubAccessPath<FieldRef>... fields) {
			HashSet<FieldRef> newSet = Sets.newHashSet(set);
			for(SubAccessPath<FieldRef> f : fields) {
				newSet.addAll(f.elements());
			}
			
			return new SetOfPossibleFieldAccesses<>(newSet);
		}
		
		@Override
		public String toString() {
			return "{"+Joiner.on(",").join(set)+"}";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((set == null) ? 0 : set.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SetOfPossibleFieldAccesses))
				return false;
			SetOfPossibleFieldAccesses other = (SetOfPossibleFieldAccesses) obj;
			if (set == null) {
				if (other.set != null)
					return false;
			} else if (!set.equals(other.set))
				return false;
			return true;
		}

		@Override
		public boolean intersects(SubAccessPath<FieldRef> accPath) {
			for(FieldRef f:set) {
				if(accPath.contains(f))
					return true;
			}
			return false;
		}

		@Override
		public boolean shouldBeMerged(FieldRef field) {
			for(FieldRef f : set) {
				if(f.shouldBeMergedWith(field))
					return true;
			}
			return false;
		}

		@Override
		public boolean shouldBeMerged(SubAccessPath<FieldRef> accPath) {
			for(FieldRef f : set) {
				if(accPath.shouldBeMerged(f))
					return true;
			}
			return false;
		}
		
		
	}

}