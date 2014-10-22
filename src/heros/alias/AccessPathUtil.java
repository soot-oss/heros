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

public class AccessPathUtil {

	public static <D extends FieldSensitiveFact<?, D>> boolean isPrefixOf(D prefixCandidate, D fact) {
		if(!prefixCandidate.getBaseValue().equals(fact.getBaseValue()))
			return false;	
		
		FieldReference[] prefixAccessPath = prefixCandidate.getAccessPath();
		FieldReference[] factAccessPath = fact.getAccessPath();
		if(prefixAccessPath.length > factAccessPath.length)
			return false;
		
		for(int i=0; i<prefixAccessPath.length; i++) {
			if(!prefixAccessPath[i].equals(factAccessPath[i]))
				return false;
		}
		
		return true;
	}
	
	public static <D extends FieldSensitiveFact<?, D>> D applyAbstractedSummary(D sourceFact, SummaryEdge<D, ?> summary) {
		if(!isPrefixOf(summary.getSourceFact(), sourceFact))
			throw new IllegalArgumentException(String.format("Source fact in given summary edge '%s' is not a prefix of the given source fact '%s'", summary, sourceFact));
		
		FieldReference[] abstractAccessPath = summary.getSourceFact().getAccessPath();
		FieldReference[] concreteAccessPath = sourceFact.getAccessPath();
		FieldReference[] targetAccessPath = summary.getTargetFact().getAccessPath();
		
		FieldReference[] resultAccessPath = new FieldReference[targetAccessPath.length + concreteAccessPath.length - abstractAccessPath.length];

		//copy old access path
		System.arraycopy(targetAccessPath, 0, resultAccessPath, 0, targetAccessPath.length);
		
		//copy delta access path that was omitted while creating the abstracted source fact
		System.arraycopy(concreteAccessPath, abstractAccessPath.length, resultAccessPath, targetAccessPath.length, concreteAccessPath.length - abstractAccessPath.length);
		
		return summary.getTargetFact().cloneWithAccessPath(resultAccessPath);
	}
}
