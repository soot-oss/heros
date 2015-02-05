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

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Optional;

public interface Transition<FieldRef> {

	int transitionToIndex();
	
	MatchResult<Transition<FieldRef>> isPrefixMatchOf(Transition<FieldRef> t);
	
	
	public static class SubAccessPathTransition<FieldRef> implements Transition<FieldRef> {
		
		private int transitionToIndex;
		private SubAccessPath<FieldRef> subAccPath;

		public SubAccessPathTransition(int transitionToIndex, SubAccessPath<FieldRef> subAccPath) {
			this.transitionToIndex = transitionToIndex;
			this.subAccPath = subAccPath;
			
		}

		@Override
		public int transitionToIndex() {
			return transitionToIndex;
		}

		@Override
		public MatchResult<Transition<FieldRef>> isPrefixMatchOf(Transition<FieldRef> t) {
			if(t instanceof SubAccessPathTransition) {
				if(subAccPath.intersects(((SubAccessPathTransition<FieldRef>) t).subAccPath))
					return new MatchResult<Transition<FieldRef>>(true, true);
			} 
			
			return new MatchResult<>(false, false);
		}
	}
	
	public static class ExclusionPathTransition<FieldRef> implements Transition<FieldRef> {
		
		private Set<FieldRef> excludedFields;
		private int transitionToIndex;

		public ExclusionPathTransition(int transitionToIndex, Set<FieldRef> excludedFields) {
			this.transitionToIndex = transitionToIndex;
			this.excludedFields = excludedFields;
		}
		
		@Override
		public int transitionToIndex() {
			return transitionToIndex;
		}

		@Override
		public MatchResult<Transition<FieldRef>> isPrefixMatchOf(Transition<FieldRef> t) {
			if(t instanceof SubAccessPathTransition) {
				if(!excludedFields.containsAll(((SubAccessPathTransition<FieldRef>) t).subAccPath.elements()))
					return new MatchResult<Transition<FieldRef>>(true, true);
			} else {
				Set<FieldRef> otherExcludedFields = ((ExclusionPathTransition<FieldRef>) t).excludedFields;
				boolean intersection = false;
				boolean containsAll = true;
				for(FieldRef field : excludedFields) {
					if(otherExcludedFields.contains(field))
						intersection = true;
					else
						containsAll = false;
				}
				return new MatchResult<>(containsAll || !intersection, containsAll);
			}
			
			return new MatchResult<>(false, false);
		}
	}
	
	public static class MatchResult<T> {
		
		private boolean match;
		private boolean guaranteedMatch;

		public MatchResult(boolean match, boolean guaranteedMatch) {
			this.match = match;
			this.guaranteedMatch = guaranteedMatch;
		}
		
		public boolean hasMatched() {
			return match;
		}
		
		public boolean isGuaranteedMatch() {
			return guaranteedMatch;
		}
	}
}
