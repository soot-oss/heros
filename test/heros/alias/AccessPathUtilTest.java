/*******************************************************************************
 * Copyright (c) 2014 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.alias;

import static heros.alias.AccessPath.PrefixTestResult.*;
import static heros.alias.AccessPathUtil.applyAbstractedSummary;
import static heros.alias.AccessPathUtil.isPrefixOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import heros.alias.AccessPath.PrefixTestResult;

import org.junit.Test;

public class AccessPathUtilTest {

	@Test
	public void testBaseValuePrefixOfFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a"), new Fact("a.f")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a.f"), new Fact("a")));
	}
	
	@Test
	public void testBaseValueIdentity() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a"), new Fact("a")));
	}
	
	@Test
	public void testFieldAccessPrefixOfFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a.b"), new Fact("a.b.c")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a.b.c"), new Fact("a.b")));
	}
	
	@Test
	public void testPrefixOfFieldAccessWithExclusion() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a.g")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a.g"), new Fact("a^f")));
	}
	
	@Test
	public void testIdentityWithExclusion() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a^f")));
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a^f,g"), new Fact("a^f,g")));
	}
	
	@Test
	public void testDifferentExclusions() {
		assertEquals(POTENTIAL_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a^g")));
		assertEquals(POTENTIAL_PREFIX, isPrefixOf(new Fact("a^f^f,g"), new Fact("a^g^f,g")));
		assertEquals(POTENTIAL_PREFIX, isPrefixOf(new Fact("a^f^f"), new Fact("a^f^g")));
	}
	
	@Test
	public void testMixedFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a.g.g")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a.f.h")));
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a.f"), new Fact("a.f^g")));
	}
	
	@Test
	public void testMultipleExclusions() {
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a^f,g"), new Fact("a^f")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a^f,g"), new Fact("a^g")));
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a^f,g")));
	}

	@Test
	public void testDifferentAccessPathLength() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a.g.h")));
	}
	
	@Test
	public void testExclusionRequiresFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a"), new Fact("a^f")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a^f"), new Fact("a")));
		
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a.f"), new Fact("a.f^g")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a.f^g"), new Fact("a.f")));
		
		assertEquals(GUARANTEED_PREFIX, isPrefixOf(new Fact("a.f"), new Fact("a.f^g^h")));
		assertEquals(NO_PREFIX, isPrefixOf(new Fact("a.f^g^h"), new Fact("a.f")));
	}
	
	@Test
	public void testAbstractedSummary() {
		assertEquals(new Fact("z.f"), applyAbstractedSummary(new Fact("a.f"), new SummaryEdge<>(new Fact("a"), null, new Fact("z"))).get());
	}
	
	@Test
	public void testAbstractedFieldAccessSummary() {
		assertEquals(new Fact("z.b.c"), applyAbstractedSummary(new Fact("a.b.c"), new SummaryEdge<>(new Fact("a.b"), null, new Fact("z.b"))).get());
	}
	
	@Test
	public void testSummaryIntroducesFieldAccess() {
		assertEquals(new Fact("z.b.c"), applyAbstractedSummary(new Fact("a.c"), new SummaryEdge<>(new Fact("a"), null, new Fact("z.b"))).get());
	}
	
	@Test
	public void testSummaryRemovesFieldAccess() {
		assertEquals(new Fact("z.c"), applyAbstractedSummary(new Fact("a.b.c"), new SummaryEdge<>(new Fact("a.b"), null, new Fact("z"))).get());
	}
	
	@Test
	public void testNonAbstractedSummary() {
		assertEquals(new Fact("z"), applyAbstractedSummary(new Fact("a"), new SummaryEdge<>(new Fact("a"), null, new Fact("z"))).get());
	}
	
	@Test
	public void testSummaryWithExcludedField() {
		assertEquals(new Fact("a.f"), applyAbstractedSummary(new Fact("a.f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a^g"))).get());
	}
	
	@Test
	public void testSummaryWithMultipleExcludedFields() {
		assertEquals(new Fact("a.f^h,i"), applyAbstractedSummary(new Fact("a.f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a^g^h,i"))).get());
		assertEquals(new Fact("a.f.f"), applyAbstractedSummary(new Fact("a.f.f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a^g^h,i"))).get());
	}
	
	@Test
	public void testIdentityForExclusions() {
		assertEquals(new Fact("a^f"), applyAbstractedSummary(new Fact("a^f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a"))).get());
		assertEquals(new Fact("a^f"), applyAbstractedSummary(new Fact("a^f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a^f"))).get());
	}
	
	@Test
	public void testMergeExclusions() {
		assertEquals(new Fact("a^f,g"), applyAbstractedSummary(new Fact("a^f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a^g"))).get());
	}
	
	@Test
	public void testNullOnImpossibleSubsumption() {
		assertFalse(applyAbstractedSummary(new Fact("a.f"), new SummaryEdge<>(new Fact("a"), null, new Fact("a^f"))).isPresent());
	}
}
