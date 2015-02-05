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

import heros.alias.SubAccessPath.SetOfPossibleFieldAccesses;
import heros.alias.SubAccessPath.SpecificFieldAccess;
import heros.alias.Transition.MatchResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@SuppressWarnings("unchecked")
public class AccessPath<T extends AccessPath.FieldRef<T>> {

	public static interface FieldRef<F> {
		boolean shouldBeMergedWith(F fieldRef);
	}
	
	public static <T extends FieldRef<T>> AccessPath<T> empty() {
		return new AccessPath<T>();
	}
	
	private final SubAccessPath<T>[] accesses;
	private final Set<T>[] exclusions;
	
	public AccessPath() {
		accesses = new SubAccessPath[0];
		exclusions = new Set[0];
	}
	
	AccessPath(SubAccessPath<T>[] accesses, Set<T>[] exclusions) {
		this.accesses = accesses;
		this.exclusions = exclusions;
	}

	public boolean isAccessInExclusions(SubAccessPath<T>... fieldReferences) {
		outer: for(int i=0; i<fieldReferences.length && i<exclusions.length; i++) {
			for(T field : fieldReferences[i].elements()) {
				if(!exclusions[i].contains(field))
					continue outer;
			}
			return true;
		}			
		return false;
	}
	
	public AccessPath<T> addFieldReference(SubAccessPath<T>... fieldReferences) {
		return addFieldReference(true, fieldReferences);
	}
	
	AccessPath<T> addFieldReference(boolean merge, SubAccessPath<T>... fieldReferences) {
		if(isAccessInExclusions(fieldReferences))
			throw new IllegalArgumentException("FieldRef "+Arrays.toString(fieldReferences)+" cannot be added to "+toString());

		if(merge) {
			boolean finiteDepth = true;
			for(int i=fieldReferences.length-1; i>=0; i--) {
				if(fieldReferences[i] instanceof SetOfPossibleFieldAccesses)
					finiteDepth = false;
					
				for(int j=0; j<accesses.length; j++) {
					if(accesses[j].shouldBeMerged(fieldReferences[i])) {
						// [..., {j-i}, ...]
						
						AccessPathBuilder builder = new AccessPathBuilder(j+fieldReferences.length-i);
						builder.keep(0, j);
						builder.merge(j, accesses.length).mergeWithLast(fieldReferences, 0, i);
						builder.append(fieldReferences, i+1, fieldReferences.length);
						if(finiteDepth)
							builder.removeExclusions(fieldReferences.length);
						else
							builder.removeExclusions(Integer.MAX_VALUE);
						return builder.build();
					}
				}
			}
		}
		
		AccessPathBuilder builder = new AccessPathBuilder(accesses.length + fieldReferences.length);
		builder.keep(0, accesses.length);
		builder.append(fieldReferences, 0, fieldReferences.length);
		builder.removeExclusions(fieldReferences.length);
		return builder.build();
	}
	
	public AccessPath<T> addFieldReference(T... fieldReferences) {
		SubAccessPath<T>[] subPath = new SubAccessPath[fieldReferences.length];
		for(int i=0; i<fieldReferences.length; i++) {
			subPath[i] = new SpecificFieldAccess<>(fieldReferences[i]);
		}
		return addFieldReference(subPath);
	}
	
	private class AccessPathBuilder {
		
		private Set<T>[] newExclusions;
		private SubAccessPath<T>[] newAccesses;
		private int currentIndex = 0;

		public AccessPathBuilder(int capacity) {
			newAccesses = new SubAccessPath[capacity];
			newExclusions = exclusions;
		}
		
		public AccessPath<T> build() {
			while(newAccesses.length > 0 && newExclusions.length > 0) {
				HashSet<T> newHashSet = Sets.newHashSet(newExclusions[0]);
				if(newAccesses[newAccesses.length-1] instanceof SetOfPossibleFieldAccesses && newHashSet.removeAll(newAccesses[newAccesses.length-1].elements())) {
					if(newHashSet.isEmpty()) {
						removeExclusions(1);
					}
					else {
						newExclusions[0] = newHashSet;
						break;
					}
				} else break;
			}
			
			return new AccessPath<>(newAccesses, newExclusions);
		}

		public void removeExclusions(int length) {
			if(length>=newExclusions.length)
				newExclusions = new Set[0];
			else
				newExclusions = Arrays.copyOfRange(newExclusions, length, newExclusions.length);
		}

		public void append(SubAccessPath<T>[] fieldReferences, int start, int endExcl) {
			for(int i=start; i<endExcl; i++) {
				newAccesses[currentIndex] = fieldReferences[i];
				currentIndex++;
				if(fieldReferences[i] instanceof SetOfPossibleFieldAccesses)
					newExclusions = new Set[0];
			}
			currentIndex+=endExcl-start;
		}

		public void mergeWithLast(SubAccessPath<T>[] fieldReferences, int start, int endExcl) {
			newAccesses[currentIndex-1].merge(Arrays.copyOfRange(fieldReferences, start, endExcl));
		}

		public AccessPathBuilder merge(int srcIndex, int destIndexExcl) {
			Set<T> set = Sets.newHashSet();
			for(int i=srcIndex; i<destIndexExcl; i++) {
				set.addAll(accesses[i].elements());
			}
			newAccesses[currentIndex] = new SubAccessPath.SetOfPossibleFieldAccesses<>(set);
			currentIndex++;
			return this;
		}

		public AccessPathBuilder keep(int srcIndex, int destIndexExcl) {
			System.arraycopy(accesses, srcIndex, newAccesses, currentIndex, destIndexExcl-srcIndex);
			currentIndex += destIndexExcl-srcIndex;
			return this;
		}

		public void append(T fieldRef) {
			newAccesses[currentIndex] = new SubAccessPath.SpecificFieldAccess<>(fieldRef);
			currentIndex++;
		}
		
	}

	public ExclusionSet getExclusions(int index) {
		return new ExclusionSet(index);
	}
	
	public AccessPath<T> prepend(T fieldRef) {
		for(int j=0; j<accesses.length; j++) {
			if(accesses[j].contains(fieldRef)) {
				// [{0-j}, ...]
				
				AccessPathBuilder builder = new AccessPathBuilder(accesses.length-j);
				builder.merge(0, j+1);
				builder.keep(j+1, accesses.length);
				return builder.build();
			}
		}
		AccessPathBuilder builder = new AccessPathBuilder(accesses.length + 1);
		builder.append(fieldRef);
		builder.keep(0, accesses.length);
		return builder.build();
	}

	public AccessPath<T> removeFirst(T field) {
		for(int i=0; i<accesses.length; i++) {
			if(accesses[i].contains(field)) {
				if(accesses[i] instanceof SpecificFieldAccess)
					return new AccessPath<T>(Arrays.copyOfRange(accesses, i+1, accesses.length), exclusions);
				else
					return this;
			}
			else if(accesses[i] instanceof SpecificFieldAccess)
				throw new IllegalStateException("Trying to remove "+field+" from "+this);
		}
		
		throw new IllegalStateException("Trying to remove "+field+" from "+this);
	}
	
	public AccessPath<T> removeFirstExclusionSetIfAvailable() {
		if(exclusions.length > 0)
			return new AccessPath<T>(accesses, Arrays.copyOfRange(exclusions, 1, exclusions.length));
		else
			return this;
	}

	public AccessPath<T> mergeExcludedFieldReference(T... fieldRef) {
		if(exclusions.length>0)
			return getExclusions(0).addExclusion(fieldRef);
		else
			return appendExcludedFieldReference(fieldRef);
	}
	
	public AccessPath<T> appendExcludedFieldReference(T... fieldReferences) {
		Set<T>[] newExclusionsArray = Arrays.copyOf(exclusions, exclusions.length+1);
		newExclusionsArray[exclusions.length] = Sets.newHashSet(fieldReferences);
		return new AccessPath<>(accesses, newExclusionsArray);
	}

	public static enum PrefixTestResult {
		GUARANTEED_PREFIX(2), POTENTIAL_PREFIX(1), NO_PREFIX(0);
		
		private int value;

		private PrefixTestResult(int value) {
			this.value = value;
		}
		
		public boolean atLeast(PrefixTestResult minimum) {
			return value >= minimum.value;
		}
	}
	
	public PrefixTestResult isPrefixOf(AccessPath<T> accPath) {
		int currIndex = 0;
		int otherIndex = 0;
		PrefixTestResult result = PrefixTestResult.GUARANTEED_PREFIX;
		
		int finalIndex = finalIndex();
		outer: while(currIndex < finalIndex) {
			Collection<Transition<T>> transitions = possibleTransitions(currIndex, true);
			Collection<Transition<T>> otherTransitions = accPath.possibleTransitions(otherIndex, true);

			for(Transition<T> transition : transitions) {
				for(Transition<T> otherTransition : otherTransitions) {
					MatchResult<Transition<T>> match = transition.isPrefixMatchOf(otherTransition);
					if(match.hasMatched()) {
						if(currIndex == transition.transitionToIndex() && otherIndex == otherTransition.transitionToIndex())
							continue;
						
						currIndex = transition.transitionToIndex();
						otherIndex = otherTransition.transitionToIndex();
						if(!match.isGuaranteedMatch())
							result = PrefixTestResult.POTENTIAL_PREFIX;
						
						continue outer;
					}
				}
			}
			return PrefixTestResult.NO_PREFIX;
		}
		
		return result;
	}
	
	private int finalIndex() {
		if(exclusions.length > 0)
			return accesses.length + exclusions.length;
		
		int finalIndex = 0;
		for(int i=0; i<accesses.length; i++) {
			if(accesses[i] instanceof SpecificFieldAccess)
				finalIndex = i+1;
		}
		return finalIndex;
	}

	private Collection<Transition<T>> possibleTransitions(int index, boolean addExclusionTransitions) {
		Collection<Transition<T>> result = Lists.newLinkedList();
		if(index < accesses.length) {
			if(accesses[index] instanceof SetOfPossibleFieldAccesses) {
				result.add(new Transition.SubAccessPathTransition<>(index, accesses[index]));
				result.addAll(possibleTransitions(index+1, addExclusionTransitions));
			}
			else
				result.add(new Transition.SubAccessPathTransition<>(index+1, accesses[index]));
		} else if(addExclusionTransitions && index - accesses.length < exclusions.length) {
			result.add(new Transition.ExclusionPathTransition<T>(index+1, exclusions[index-accesses.length]));
		}
		return result;
	}
	
	public SubAccessPath<T>[] getDeltaTo(AccessPath<T> accPath) {
		int currIndex = 0;
		int otherIndex = 0;
		
		outer: while(true) {
			Collection<Transition<T>> transitions = possibleTransitions(currIndex, false);
			Collection<Transition<T>> otherTransitions = accPath.possibleTransitions(otherIndex, false);

			for(Transition<T> transition : transitions) {
				for(Transition<T> otherTransition : otherTransitions) {
					MatchResult<Transition<T>> match = transition.isPrefixMatchOf(otherTransition);
					if(match.hasMatched()) {
						if(currIndex == transition.transitionToIndex() && otherIndex == otherTransition.transitionToIndex())
							continue;
						
						currIndex = transition.transitionToIndex();
						otherIndex = otherTransition.transitionToIndex();
						continue outer;
					}
				}
			}
			break;
		}
		
		return Arrays.copyOfRange(accPath.accesses, otherIndex, accPath.accesses.length);
		
//		int currentIndex = 0;
//		for(SubAccessPath<T> sub : accesses) {
//			if(!(sub instanceof SpecificFieldAccess))
//				throw new IllegalArgumentException("Cannot calculate delta to. Current AccessPath contains set elements: "+toString());
//			
//			T field = sub.elements().iterator().next();
//			
//			while(true) {
//				if(currentIndex<accPath.accesses.length && accPath.accesses[currentIndex].contains(field)) {
//					if(accPath.accesses[currentIndex] instanceof SpecificFieldAccess)
//						currentIndex++;
//					break;
//				} else if(currentIndex<accPath.accesses.length && accPath.accesses[currentIndex] instanceof SetOfPossibleFieldAccesses) {
//					currentIndex++;
//				}
//				else
//					throw new IllegalArgumentException("'"+toString()+ "' is not a prefix of the given AccessPath: "+accPath);
//			}
//		}
//		
//		return Arrays.copyOfRange(accPath.accesses, currentIndex, accPath.accesses.length);
	}
	
	public AccessPath<T> mergeExcludedFieldReferences(AccessPath<T> accPath) {
		Set<T>[] newExclusionArray = new Set[Math.max(exclusions.length,accPath.exclusions.length)];
		for(int i=0; i<newExclusionArray.length; i++) {
			newExclusionArray[i] = Sets.newHashSet();
			if(i<exclusions.length)
				newExclusionArray[i].addAll(exclusions[i]);
			if(i<accPath.exclusions.length)
				newExclusionArray[i].addAll(accPath.exclusions[i]);
		}
		return new AccessPath<>(accesses, newExclusionArray);
	}
	
	public boolean mayHaveEmptyAccessPath() {
		return finalIndex() == 0;
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
		for(Set<T> exclusion : exclusions) {
			result += "^" + Joiner.on(",").join(exclusion);
		}
		return result;
	}
	
//	public <T> AccessPath<T> map(Function<FieldRef, T> function) {
//		T[] newAccesses = (T[]) new Object[accesses.length];
//		for(int i=0; i<accesses.length; i++) {
//			newAccesses[i] = function.apply(accesses[i]);
//		}
//		Set<T>[] newExclusions = new Set[exclusions.length];
//		for(int i=0; i<exclusions.length; i++) {
//			newExclusions[i] = Sets.newHashSet();
//			for(FieldRef excl : exclusions[i]) {
//				newExclusions[i].add(function.apply(excl));
//			}
//		}
//		return new AccessPath<T>(newAccesses, newExclusions);
//	}
	
	public class ExclusionSet {
		private int index;
	
		private ExclusionSet(int index) {
			this.index = index;
		}
		
		public AccessPath<T> addExclusion(T... exclusion) {
			HashSet<T> newExclusions = Sets.newHashSet(exclusions[index]);
			for(T excl : exclusion)
				newExclusions.add(excl);
			Set<T>[] newExclusionsArray = exclusions.clone();
			newExclusionsArray[index] = newExclusions;
			return new AccessPath<T>(accesses, newExclusionsArray);
		}
	}

	public AccessPath<T> removeAnyAccess() {
		if(accesses.length > 0)
			return new AccessPath<T>(new SubAccessPath[0], exclusions);
		else
			return this;
	}

	public boolean hasEmptyAccessPath() {
		return accesses.length == 0;
	}
}
