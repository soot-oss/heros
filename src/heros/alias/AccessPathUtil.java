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

import java.util.Set;

import heros.alias.AccessPath.PrefixTestResult;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;


public class AccessPathUtil {

	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef,  D>> PrefixTestResult isPrefixOf(D prefixCandidate, D fact) {
		if(prefixCandidate.getBaseValue() == null) {
			if(fact.getBaseValue() != null)
				return PrefixTestResult.NO_PREFIX;
		} else if(!prefixCandidate.getBaseValue().equals(fact.getBaseValue()))
			return PrefixTestResult.NO_PREFIX;
		
		return prefixCandidate.getAccessPath().isPrefixOf(fact.getAccessPath());
	}

	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef, D>> Optional<D> applyAbstractedSummary(D sourceFact, SummaryEdge<D, ?> summary) {
		if(!isPrefixOf(summary.getSourceFact(), sourceFact).atLeast(PrefixTestResult.GUARANTEED_PREFIX))
			throw new IllegalArgumentException(String.format("Source fact in given summary edge '%s' is not a prefix of the given source fact '%s'", summary, sourceFact));
		
		AccessPath<FieldRef> concreteAccessPath = sourceFact.getAccessPath();
		AccessPath<FieldRef> abstractAccessPath = summary.getSourceFact().getAccessPath();
		AccessPath<FieldRef> targetAccessPath = summary.getTargetFact().getAccessPath();

		if(abstractAccessPath.equals(concreteAccessPath))
			return Optional.of(summary.getTargetFact());
		
		SubAccessPath<FieldRef>[] delta = abstractAccessPath.getDeltaTo(concreteAccessPath);
		if(targetAccessPath.isAccessInExclusions(delta))
			return Optional.absent();
		
		AccessPath<FieldRef> result = targetAccessPath.addFieldReference(delta);
		result = result.mergeExcludedFieldReferences(concreteAccessPath);
		
		return Optional.of(summary.getTargetFact().cloneWithAccessPath(result));
	}

	public static <FieldRef, D extends FieldSensitiveFact<?, FieldRef,  D>> D cloneWithAccessPath(D fact, AccessPath<FieldRef> accPath) {
		if(fact.getAccessPath().equals(accPath))
			return fact;
		else
			return fact.cloneWithAccessPath(accPath);
	}
	
}
