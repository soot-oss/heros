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
import heros.alias.SubAccessPath.SetOfPossibleFieldAccesses;
import heros.alias.SubAccessPath.SpecificFieldAccess;
import heros.alias.Transition.MatchResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
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
	private final Set<T> exclusions;
	
	public AccessPath() {
		accesses = new SubAccessPath[0];
		exclusions = Sets.newHashSet();
	}
	
	AccessPath(SubAccessPath<T>[] accesses, Set<T> exclusions) {
		this.accesses = accesses;
		this.exclusions = exclusions;
	}

	public boolean isAccessInExclusions(T fieldReferences) {
		return exclusions.contains(fieldReferences);
	}
	
	public boolean isAccessInExclusions(SubAccessPath<T>... fieldReferences) {
		if(fieldReferences.length > 0) {
			for(T field : fieldReferences[0].elements()) {
				if(!exclusions.contains(field))
					return false;
			}
			return true;
		}
		return false;
	}
	
	public AccessPath<T> addFieldReference(SubAccessPath<T>... fieldReferences) {
		return addFieldReference(true, fieldReferences);
	}

	public boolean hasAllExclusionsOf(AccessPath<T> accPath) {
		return exclusions.containsAll(accPath.exclusions);
	}
	
	AccessPath<T> addFieldReference(boolean merge, SubAccessPath<T>... fieldReferences) {
		if(isAccessInExclusions(fieldReferences))
			throw new IllegalArgumentException("FieldRef "+Arrays.toString(fieldReferences)+" cannot be added to "+toString());

		//FIXME do we need to not merge sometimes?
//		if(merge) {
//			for(int i=fieldReferences.length-1; i>=0; i--) {
//				for(int j=0; j<accesses.length; j++) {
//					if(accesses[j].shouldBeMerged(fieldReferences[i])) {
//						// [..., {j-i}, ...]
//						
//						AccessPathBuilder builder = new AccessPathBuilder(j+fieldReferences.length-i);
//						builder.keep(0, j);
//						builder.merge(j, accesses.length).mergeWithLast(fieldReferences, 0, i);
//						builder.append(fieldReferences, i+1, fieldReferences.length);
//						builder.removeExclusions();
//						return builder.build();
//					}
//				}
//			}
//		}
		
		AccessPathBuilder builder = new AccessPathBuilder(accesses.length + fieldReferences.length);
		builder.keep(0, accesses.length);
		builder.append(fieldReferences, 0, fieldReferences.length);
		if(fieldReferences.length>0)
			builder.removeExclusions();
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
		
		private Set<T> newExclusions;
		private SubAccessPath<T>[] newAccesses;
		private int currentIndex = 0;

		public AccessPathBuilder(int capacity) {
			newAccesses = new SubAccessPath[capacity];
			newExclusions = exclusions;
		}
		
		public AccessPath<T> build() {
			return new AccessPath<>(newAccesses, newExclusions);
		}

		public void removeExclusions() {
			newExclusions = Sets.newHashSet();
		}

		public void append(SubAccessPath<T>[] fieldReferences, int start, int endExcl) {
			for(int i=start; i<endExcl; i++) {
				newAccesses[currentIndex] = fieldReferences[i];
				currentIndex++;
				if(fieldReferences[i] instanceof SetOfPossibleFieldAccesses)
					removeExclusions();
			}
			currentIndex+=endExcl-start;
		}

		public void mergeWithLast(SubAccessPath<T>[] fieldReferences, int start, int endExcl) {
			newAccesses[currentIndex-1] = newAccesses[currentIndex-1].merge(Arrays.copyOfRange(fieldReferences, start, endExcl));
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

	public AccessPath<T> prepend(T fieldRef) {
//		for(int j=0; j<accesses.length; j++) {
//			if(accesses[j].contains(fieldRef)) {
//				// [{0-j}, ...]
//				
//				AccessPathBuilder builder = new AccessPathBuilder(accesses.length-j);
//				builder.merge(0, j+1);
//				builder.keep(j+1, accesses.length);
//				return builder.build();
//			}
//		}
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
	
	public AccessPath<T> appendExcludedFieldReference(T... fieldReferences) {
		HashSet<T> newExclusions = Sets.newHashSet(fieldReferences);
		newExclusions.addAll(exclusions);
		return new AccessPath<>(accesses, newExclusions);
	}

	public AccessPath<T> appendExcludedFieldReference(Collection<T> fieldReferences) {
		HashSet<T> newExclusions = Sets.newHashSet(fieldReferences);
		newExclusions.addAll(exclusions);
		return new AccessPath<>(accesses, newExclusions);
	}

	public static enum PrefixTestResult {
		GUARANTEED_PREFIX(3), POTENTIAL_PREFIX(2), NEEDS_RESOLVNG(1), NO_PREFIX(0);
		
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
		if(!exclusions.isEmpty())
			return accesses.length + 1;
		
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
		} else if(addExclusionTransitions && index - accesses.length == 0 && !exclusions.isEmpty()) {
			result.add(new Transition.ExclusionPathTransition<T>(index+1, exclusions));
		}
		return result;
	}
	
	public Delta<T> getDeltaTo(AccessPath<T> accPath) {
		assert isPrefixOf(accPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX);
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
//		Set<T> exclusions = Sets.newHashSet(accPath.exclusions);
//		exclusions.removeAll(this.exclusions);
		Delta<T> delta = new Delta<T>(Arrays.copyOfRange(accPath.accesses, otherIndex, accPath.accesses.length), accPath.exclusions);
		assert (isPrefixOf(accPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX) && accPath.isPrefixOf(delta.applyTo(this, true)) == PrefixTestResult.GUARANTEED_PREFIX) 
				|| (isPrefixOf(accPath) == PrefixTestResult.GUARANTEED_PREFIX && accPath.equals(delta.applyTo(this, true)));
		return delta;
	}
	
	public boolean contains(AccessPath<T> accPath) {
		assert accPath.accesses.length <= 1;
		if(accPath.accesses.length == 1) {
			for(SubAccessPath<T> sub : accesses) {
				if(sub.elements().equals(accPath.accesses[0].elements())) {
					return true;
				}
			}
			return false;
		}
		else
			return exclusions.containsAll(accPath.exclusions);
	}
	
	
	public static class Delta<T extends FieldRef<T>> {
		final SubAccessPath<T>[] accesses;
		final Set<T> exclusions;

		protected Delta(SubAccessPath<T>[] accesses, Set<T> exclusions) {
			this.accesses = accesses;
			this.exclusions = exclusions;
		}
		
		public boolean canBeAppliedTo(AccessPath<T> accPath) {
			return !accPath.isAccessInExclusions(accesses);
		}
		
		public AccessPath<T> applyTo(AccessPath<T> accPath, boolean merge) {
			return accPath.addFieldReference(merge, accesses).appendExcludedFieldReference(exclusions);
		}
		
		@Override
		public String toString() {
			String result = accesses.length > 0 ? "."+Joiner.on(".").join(accesses) : "";
			if(!exclusions.isEmpty())
				result += "^" + Joiner.on(",").join(exclusions);
			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(accesses);
			result = prime * result + ((exclusions == null) ? 0 : exclusions.hashCode());
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
			Delta other = (Delta) obj;
			if (!Arrays.equals(accesses, other.accesses))
				return false;
			if (exclusions == null) {
				if (other.exclusions != null)
					return false;
			} else if (!exclusions.equals(other.exclusions))
				return false;
			return true;
		}

		public static <T extends FieldRef<T>> Delta<T> empty() {
			return new Delta<T>(new SubAccessPath[0], Sets.<T>newHashSet());
		}
	}
	
	public AccessPath<T> mergeExcludedFieldReferences(AccessPath<T> accPath) {
		HashSet<T> newExclusions = Sets.newHashSet(exclusions);
		newExclusions.addAll(accPath.exclusions);
		return new AccessPath<>(accesses, newExclusions);
	}
	
	public boolean mayHaveEmptyAccessPath() {
		for(SubAccessPath<T> subAcc : accesses)
			if(subAcc instanceof SpecificFieldAccess)
				return false;
		return true;
	}
	
	public boolean canRead(T field) {
		for(SubAccessPath<T> acc : accesses) {
			if(acc.contains(field))
				return true;
			if(acc instanceof SpecificFieldAccess)
				return false;
		}
		return false;
	}
	
	public boolean isEmpty() {
		return exclusions.isEmpty() && accesses.length == 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(accesses);
		result = prime * result + ((exclusions == null) ? 0 : exclusions.hashCode());
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
		AccessPath other = (AccessPath) obj;
		if (!Arrays.equals(accesses, other.accesses))
			return false;
		if (exclusions == null) {
			if (other.exclusions != null)
				return false;
		} else if (!exclusions.equals(other.exclusions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String result = accesses.length > 0 ? "."+Joiner.on(".").join(accesses) : "";
		if(!exclusions.isEmpty())
			result += "^" + Joiner.on(",").join(exclusions);
		return result;
	}
	
	public <U extends FieldRef<U>> AccessPath<U> map(Function<T, U> function) {
		SubAccessPath<U>[] newAccesses = new SubAccessPath[accesses.length];
		for(int i=0; i<accesses.length; i++) {
			newAccesses[i] = accesses[i].map(function);
		}
		Set<U> newExclusions = Sets.newHashSet();
		for(T f : exclusions)
			newExclusions.add(function.apply(f));
		return new AccessPath<U>(newAccesses, newExclusions);
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

	public boolean subsumes(AccessPath<T> accPath) {
		int currIndex = 0;
		int otherIndex = 0;
		
		outer: while(true) {
			Collection<Transition<T>> transitions = possibleTransitions(currIndex, false);
			Collection<Transition<T>> otherTransitions = accPath.possibleTransitions(otherIndex, false);

			if((currIndex >= accesses.length || (currIndex == accesses.length-1 && accesses[currIndex] instanceof SetOfPossibleFieldAccesses)) 
					&& otherIndex>=accPath.accesses.length-1) {
				if(transitions.isEmpty())
					return otherTransitions.isEmpty() && hasAtLeastTheSameExclusionsAs(accPath);
				for(Transition<T> transition : transitions) {
					for(Transition<T> otherTransition : otherTransitions) {
						MatchResult<Transition<T>> match = transition.isPrefixMatchOf(otherTransition);
						if(!match.hasMatched())
							return false;
					}	
				}
				return hasAtLeastTheSameExclusionsAs(accPath);
			}

			for(Transition<T> transition : transitions) {
				for(Transition<T> otherTransition : otherTransitions) {
					MatchResult<Transition<T>> match = transition.isPrefixMatchOf(otherTransition);
					if(match.hasMatched()) {
						if(otherIndex == otherTransition.transitionToIndex())
							continue;
						
						currIndex = transition.transitionToIndex();
						otherIndex = otherTransition.transitionToIndex();
						continue outer;
					}
				}
			}
			return false;
		}
	}
	
	private boolean hasAtLeastTheSameExclusionsAs(AccessPath<T> accPath) {
		return accPath.exclusions.containsAll(exclusions);
	}

	public Collection<String> tokenize() {
		List<String> result = Lists.newLinkedList();
		for(SubAccessPath<T> s : accesses) {
			result.add(s.toString());
		}
		if(!exclusions.isEmpty())
			result.add("^"+Joiner.on(",").join(exclusions));
		return result;
	}

	public AccessPath<T> removeExclusions() {
		return new AccessPath<T>(accesses, Sets.<T>newHashSet());
	}

	public SubAccessPath<T> getFirstAccess() {
		return accesses[0];
	}

	public AccessPath<T> removeRepeatableFirstAccess(T field) {
		Collection<? extends T> elements = accesses[0].elements();
		if(!elements.contains(field))
			throw new IllegalArgumentException();
		
		if(elements.size() == 1) {
			return new AccessPath<>(Arrays.copyOfRange(accesses, 1, accesses.length), exclusions);
		}
		
		HashSet<T> newSet = Sets.newHashSet(elements);
		newSet.remove(field);
		SubAccessPath<T>[] newAccesses = Arrays.copyOf(accesses, accesses.length);
		newAccesses[0] = new SetOfPossibleFieldAccesses<>(newSet);
		return new AccessPath<T>(newAccesses, exclusions);
	}

	public class Iterator {
		private int currentIndex = 0;
		
		public boolean hasNext(T field) {
			for(int i=0; i+currentIndex < accesses.length; i++) {
				if(accesses[currentIndex+i].contains(field))
					return true;
				if(accesses[currentIndex+i] instanceof SpecificFieldAccess)
					return false;
			}
			return false;
		}
		
		public void next(T field) {
			for(int i=0; i+currentIndex < accesses.length; i++) {
				if(accesses[currentIndex+i].contains(field)) {
					currentIndex+=i;
					return;
				}
				if(accesses[currentIndex+i] instanceof SpecificFieldAccess)
					throw new IllegalStateException();
			}
			throw new IllegalStateException();
		}
		
		public boolean maybeAtEnd() {
			for(int i=0; i+currentIndex < accesses.length; i++) {
				if(accesses[currentIndex+i] instanceof SpecificFieldAccess)
					return false;
			}
			return true;
		}
		
		public boolean isExcluded(T field) {
			return exclusions.contains(field);
		}
		
	}

	public AccessPath<T>.Iterator iterator() {
		return new Iterator();
	}
}
