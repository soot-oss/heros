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


public class AccessPathUtil {

	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef,  D>> boolean isPrefixOf(D prefixCandidate, D fact) {
		if(prefixCandidate.getBaseValue() == null) {
			if(fact.getBaseValue() != null)
				return false;
		} else if(!prefixCandidate.getBaseValue().equals(fact.getBaseValue()))
			return false;
		
		return prefixCandidate.getAccessPath().isPrefixOf(fact.getAccessPath());
	}
	
	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef, D>> Optional<D> applyAbstractedSummary(D sourceFact, SummaryEdge<D, ?> summary) {
		if(!isPrefixOf(summary.getSourceFact(), sourceFact))
			throw new IllegalArgumentException(String.format("Source fact in given summary edge '%s' is not a prefix of the given source fact '%s'", summary, sourceFact));
		
		AccessPath<FieldRef> concreteAccessPath = sourceFact.getAccessPath();
		AccessPath<FieldRef> abstractAccessPath = summary.getSourceFact().getAccessPath();
		AccessPath<FieldRef> targetAccessPath = summary.getTargetFact().getAccessPath();
		
		FieldRef[] delta = abstractAccessPath.getDeltaTo(concreteAccessPath);
		if(targetAccessPath.isAccessInExclusions(delta))
			return Optional.absent();
		
		AccessPath<FieldRef> result = targetAccessPath.addFieldReference(delta);
		result = result.mergeExcludedFieldReferences(concreteAccessPath);
		
		return Optional.of(summary.getTargetFact().cloneWithAccessPath(result));
	}

//	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef, D>> D cloneWithConcatenatedAccessPath(D fact, FieldReference... fieldRefs) {
//		FieldReference[] accessPath = new FieldReference[fact.getAccessPath().length+fieldRefs.length];
//		System.arraycopy(fact.getAccessPath(), 0, accessPath, 0, fact.getAccessPath().length);
//		System.arraycopy(fieldRefs, 0, accessPath, fact.getAccessPath().length, fieldRefs.length);
//		return fact.cloneWithAccessPath(accessPath);
//	}
	
//	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef, D>> D concretizeCallerSourceFact(IncomingEdge<D, ?> incomingEdge, D calleeSourceFact) {
//		if(!isPrefixOf(incomingEdge.getCalleeSourceFact(), calleeSourceFact))
//			throw new IllegalArgumentException(String.format("Callee Source Fact in IncomingEdge '%s' is not a prefix of the given fact '%s'.", incomingEdge, calleeSourceFact));
//		
//		FieldReference[] abstractAccessPath = incomingEdge.getCalleeSourceFact().getAccessPath();
//		FieldReference[] concreteAccessPath = calleeSourceFact.getAccessPath();
//		FieldReference[] targetAccessPath = incomingEdge.getCallerSourceFact().getAccessPath();
//		
//		FieldReference[] resultAccessPath = new FieldReference[targetAccessPath.length + concreteAccessPath.length - abstractAccessPath.length];
//
//		//copy old access path
//		System.arraycopy(targetAccessPath, 0, resultAccessPath, 0, targetAccessPath.length);
//		
//		//copy delta access path that was omitted while creating the abstracted source fact
//		System.arraycopy(concreteAccessPath, abstractAccessPath.length, resultAccessPath, targetAccessPath.length, concreteAccessPath.length - abstractAccessPath.length);
//		
//		return incomingEdge.getCallerSourceFact().cloneWithAccessPath(resultAccessPath);
//	}
}
