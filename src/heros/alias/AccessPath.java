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
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

@SuppressWarnings("unchecked")
public class AccessPath<FieldRef> {

	private final FieldRef[] accesses;
	private final Set<FieldRef>[] exclusions;
	
	public AccessPath() {
		accesses = (FieldRef[]) new Object[0];
		exclusions = new Set[0];
	}
	
	AccessPath(FieldRef[] accesses, Set<FieldRef>[] exclusions) {
		int k = 3;
		if(accesses.length > k) {
			this.accesses = Arrays.copyOf(accesses, k);
			this.exclusions = new Set[0];
		}
		else {
			this.accesses = accesses;
			if(exclusions.length > k - accesses.length)
				this.exclusions = Arrays.copyOf(exclusions, k - accesses.length);
			else
				this.exclusions = exclusions;
		}
	}

	public boolean hasExclusions() {
		return exclusions.length > 0;
	}
	
	public boolean isAccessInExclusions(FieldRef... fieldReferences) {
		for(int i=0; i<fieldReferences.length && i<exclusions.length; i++) {
			if(exclusions[i].contains(fieldReferences[i]))
				return true;
		}			
		return false;
	}
	
	public AccessPath<FieldRef> addFieldReference(FieldRef... fieldReferences) {
		if(isAccessInExclusions(fieldReferences))
			throw new IllegalArgumentException("FieldRef "+Arrays.toString(fieldReferences)+" cannot be added to "+toString());

		FieldRef[] newAccesses = Arrays.copyOf(accesses, accesses.length+fieldReferences.length);
		System.arraycopy(fieldReferences, 0, newAccesses, accesses.length, fieldReferences.length);
		Set<FieldRef>[] newExclusionsArray = exclusions.length < fieldReferences.length ? exclusions : Arrays.copyOfRange(exclusions, fieldReferences.length, exclusions.length);			
		return new AccessPath<FieldRef>(newAccesses, newExclusionsArray);
	}

	public ExclusionSet getExclusions(int index) {
		return new ExclusionSet(index);
	}
	
	public AccessPath<FieldRef> append(AccessPath<FieldRef> accessPath) {
		if(exclusions.length > 0) 
			throw new IllegalStateException();
		
		FieldRef[] newAccesses = Arrays.copyOf(accesses, accesses.length + accessPath.accesses.length);
		System.arraycopy(accessPath.accesses, 0, newAccesses, accesses.length, accessPath.accesses.length);
		return new AccessPath<FieldRef>(newAccesses, accessPath.exclusions);
	}

	public AccessPath<FieldRef> removeFirstAccessIfAvailable() {
		if(accesses.length > 0)
			return new AccessPath<FieldRef>(Arrays.copyOfRange(accesses, 1, accesses.length), exclusions);
		else if(exclusions.length > 0)
			return new AccessPath<FieldRef>(accesses, Arrays.copyOfRange(exclusions, 1, exclusions.length));
		else
			return this;
	}

	public AccessPath<FieldRef> mergeExcludedFieldReference(FieldRef... fieldRef) {
		if(hasExclusions())
			return getExclusions(0).addExclusion(fieldRef);
		else
			return appendExcludedFieldReference(fieldRef);
	}
	
	public AccessPath<FieldRef> appendExcludedFieldReference(FieldRef... fieldReferences) {
		Set<FieldRef>[] newExclusionsArray = Arrays.copyOf(exclusions, exclusions.length+1);
		newExclusionsArray[exclusions.length] = Sets.newHashSet(fieldReferences);
		return new AccessPath<>(accesses, newExclusionsArray);
	}
	
	public boolean isPrefixOf(AccessPath<FieldRef> accessPath) {
		if(accesses.length > accessPath.accesses.length)
			return false;
		
		if(accesses.length + exclusions.length > accessPath.accesses.length + accessPath.exclusions.length)
			return false;
		
		for(int i=0; i<accesses.length; i++) {
			if(!accesses[i].equals(accessPath.accesses[i]))
				return false;
		}
		
		for(int i=0; i<exclusions.length; i++) {
			if(i+accesses.length < accessPath.accesses.length) {
				if(exclusions[i].contains(accessPath.accesses[i+accesses.length]))
					return false;
			}
			else {
				if(!exclusions[i].containsAll(accessPath.exclusions[i+accesses.length - accessPath.accesses.length]))
					return false;
			}
		}
		
		return true;
	}
	
	public FieldRef[] getDeltaTo(AccessPath<FieldRef> accPath) {
		if(isPrefixOf(accPath))
			return Arrays.copyOfRange(accPath.accesses, accesses.length, accPath.accesses.length);
		else
			throw new IllegalArgumentException("Given AccessPath must be a prefix of the current AccessPath");
	}
	
	public AccessPath<FieldRef> mergeExcludedFieldReferences(AccessPath<FieldRef> accPath) {
		Set<FieldRef>[] newExclusionArray = new Set[Math.max(exclusions.length,accPath.exclusions.length)];
		for(int i=0; i<newExclusionArray.length; i++) {
			newExclusionArray[i] = Sets.newHashSet();
			if(i<exclusions.length)
				newExclusionArray[i].addAll(exclusions[i]);
			if(i<accPath.exclusions.length)
				newExclusionArray[i].addAll(accPath.exclusions[i]);
		}
		return new AccessPath<>(accesses, newExclusionArray);
	}
	
	public boolean isEmpty() {
		return exclusions.length == 0 && accesses.length == 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(accesses);
		result = prime * result + Arrays.hashCode(exclusions);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AccessPath))
			return false;
		AccessPath other = (AccessPath) obj;
		if (!Arrays.equals(accesses, other.accesses))
			return false;
		if (!Arrays.equals(exclusions, other.exclusions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String result = accesses.length > 0 ? "."+Joiner.on(".").join(accesses) : "";
		for(Set<FieldRef> exclusion : exclusions) {
			result += "^" + Joiner.on(",").join(exclusion);
		}
		return result;
	}
	
	public <T> AccessPath<T> map(Function<FieldRef, T> function) {
		T[] newAccesses = (T[]) new Object[accesses.length];
		for(int i=0; i<accesses.length; i++) {
			newAccesses[i] = function.apply(accesses[i]);
		}
		Set<T>[] newExclusions = new Set[exclusions.length];
		for(int i=0; i<exclusions.length; i++) {
			newExclusions[i] = Sets.newHashSet();
			for(FieldRef excl : exclusions[i]) {
				newExclusions[i].add(function.apply(excl));
			}
		}
		return new AccessPath<T>(newAccesses, newExclusions);
	}
	
	public class ExclusionSet {
		private int index;
	
		private ExclusionSet(int index) {
			this.index = index;
		}
		
		public AccessPath<FieldRef> addExclusion(FieldRef... exclusion) {
			HashSet<FieldRef> newExclusions = Sets.newHashSet(exclusions[index]);
			for(FieldRef excl : exclusion)
				newExclusions.add(excl);
			Set<FieldRef>[] newExclusionsArray = exclusions.clone();
			newExclusionsArray[index] = newExclusions;
			return new AccessPath<FieldRef>(accesses, newExclusionsArray);
		}
	}
}
