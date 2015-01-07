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

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

@SuppressWarnings("unchecked")
public class AccessPath<FieldRef> {

	private FieldRef[] accesses;
	private Set<FieldRef>[] exclusions;
	
	public AccessPath() {
		accesses = (FieldRef[]) new Object[0];
		exclusions = new Set[0];
	}
	
	AccessPath(FieldRef[] accesses, Set<FieldRef>[] exclusions) {
		this.accesses = accesses;
		this.exclusions = exclusions;
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
			throw new IllegalArgumentException();

		FieldRef[] newAccesses = Arrays.copyOf(accesses, accesses.length+fieldReferences.length);
		System.arraycopy(fieldReferences, 0, newAccesses, accesses.length, fieldReferences.length);
		Set<FieldRef>[] newExclusionsArray = exclusions.length < fieldReferences.length ? exclusions : Arrays.copyOfRange(exclusions, fieldReferences.length, exclusions.length);			
		return new AccessPath<FieldRef>(newAccesses, newExclusionsArray);
	}

	public ExclusionSet getExclusions(int index) {
		return new ExclusionSet(index);
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
	
	public class ExclusionSet {
		private int index;
	
		private ExclusionSet(int index) {
			this.index = index;
		}
		
		public AccessPath<FieldRef> addExclusion(FieldRef exclusion) {
			HashSet<FieldRef> newExclusions = Sets.newHashSet(exclusions[index]);
			newExclusions.add(exclusion);
			Set<FieldRef>[] newExclusionsArray = exclusions.clone();
			newExclusionsArray[index] = newExclusions;
			return new AccessPath<FieldRef>(accesses, newExclusionsArray);
		}
	}
}
