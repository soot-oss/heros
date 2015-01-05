/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.alias;

import java.util.ArrayList;

import com.google.common.base.Optional;

import heros.alias.FieldReference.Any;
import heros.alias.FieldReference.SpecificFieldReference;

public class AccessPathUtil {

	public static <D extends FieldSensitiveFact<?, D>> boolean isPrefixOf(D prefixCandidate, D fact) {
		if(!prefixCandidate.getBaseValue().equals(fact.getBaseValue()))
			return false;	
		
		FieldReference[] prefixAccessPath = prefixCandidate.getAccessPath();
		FieldReference[] factAccessPath = fact.getAccessPath();
		
		for(int i=0; i<prefixAccessPath.length; i++) {
			if(i < factAccessPath.length) {
				if(!prefixAccessPath[i].includes(factAccessPath[i]))
					return false;
			}
			else if(!(prefixAccessPath[i] instanceof Any))
				return false;	
		}
		
		return true;
	}
	
	public static <D extends FieldSensitiveFact<?, D>> Optional<D> applyAbstractedSummary(D sourceFact, SummaryEdge<D, ?> summary) {
		if(!isPrefixOf(summary.getSourceFact(), sourceFact))
			throw new IllegalArgumentException(String.format("Source fact in given summary edge '%s' is not a prefix of the given source fact '%s'", summary, sourceFact));
		
		FieldReference[] concreteAccessPath = sourceFact.getAccessPath();
		FieldReference[] abstractAccessPath = summary.getSourceFact().getAccessPath();
		FieldReference[] targetAccessPath = summary.getTargetFact().getAccessPath();
		
		
		ArrayList<FieldReference> result = new ArrayList<>(targetAccessPath.length + concreteAccessPath.length - abstractAccessPath.length);
		int lastSpecificField = -1;
		for(int i=0; i< targetAccessPath.length; i++) {
			result.add(targetAccessPath[i]);
			if(targetAccessPath[i] instanceof SpecificFieldReference)
				lastSpecificField = i;
		}
		
		for(int i=abstractAccessPath.length; i<concreteAccessPath.length; i++) {
			if(lastSpecificField+1 < result.size()) {
				Optional<? extends FieldReference> mergedFieldRef = concreteAccessPath[i].merge((Any) result.get(lastSpecificField+1));
				if(!mergedFieldRef.isPresent())
					return Optional.absent();
				
				result.set(lastSpecificField+1, mergedFieldRef.get());
				lastSpecificField++;
			} else {
				result.add(concreteAccessPath[i]);
			}
		}
		return Optional.of(summary.getTargetFact().cloneWithAccessPath(result.toArray(new FieldReference[result.size()])));
	}

	public static <D extends FieldSensitiveFact<?, D>> D cloneWithConcatenatedAccessPath(D fact, FieldReference... fieldRefs) {
		FieldReference[] accessPath = new FieldReference[fact.getAccessPath().length+fieldRefs.length];
		System.arraycopy(fact.getAccessPath(), 0, accessPath, 0, fact.getAccessPath().length);
		System.arraycopy(fieldRefs, 0, accessPath, fact.getAccessPath().length, fieldRefs.length);
		return fact.cloneWithAccessPath(accessPath);
	}
	
	public static <D extends FieldSensitiveFact<?, D>> D concretizeCallerSourceFact(IncomingEdge<D, ?> incomingEdge, D calleeSourceFact) {
		if(!isPrefixOf(incomingEdge.getCalleeSourceFact(), calleeSourceFact))
			throw new IllegalArgumentException(String.format("Callee Source Fact in IncomingEdge '%s' is not a prefix of the given fact '%s'.", incomingEdge, calleeSourceFact));
		
		FieldReference[] abstractAccessPath = incomingEdge.getCalleeSourceFact().getAccessPath();
		FieldReference[] concreteAccessPath = calleeSourceFact.getAccessPath();
		FieldReference[] targetAccessPath = incomingEdge.getCallerSourceFact().getAccessPath();
		
		FieldReference[] resultAccessPath = new FieldReference[targetAccessPath.length + concreteAccessPath.length - abstractAccessPath.length];

		//copy old access path
		System.arraycopy(targetAccessPath, 0, resultAccessPath, 0, targetAccessPath.length);
		
		//copy delta access path that was omitted while creating the abstracted source fact
		System.arraycopy(concreteAccessPath, abstractAccessPath.length, resultAccessPath, targetAccessPath.length, concreteAccessPath.length - abstractAccessPath.length);
		
		return incomingEdge.getCallerSourceFact().cloneWithAccessPath(resultAccessPath);
	}
	
	public static FieldReference[] getAccessPathDelta(FieldReference[] prefixAccessPath, FieldReference[] accessPath) {
		FieldReference[] result = new FieldReference[accessPath.length - prefixAccessPath.length];
		System.arraycopy(accessPath, prefixAccessPath.length, result, 0, result.length);
		return result;
	}
}
