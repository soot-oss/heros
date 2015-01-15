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

import static heros.alias.AccessPathUtil.applyAbstractedSummary;
import static heros.alias.AccessPathUtil.isPrefixOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class AccessPathUtilTest {

	@Test
	public void testBaseValuePrefixOfFieldAccess() {
		assertTrue(isPrefixOf(new Fact("a"), new Fact("a.f")));
		assertFalse(isPrefixOf(new Fact("a.f"), new Fact("a")));
	}
	
	@Test
	public void testBaseValueIdentity() {
		assertTrue(isPrefixOf(new Fact("a"), new Fact("a")));
	}
	
	@Test
	public void testFieldAccessPrefixOfFieldAccess() {
		assertTrue(isPrefixOf(new Fact("a.b"), new Fact("a.b.c")));
		assertFalse(isPrefixOf(new Fact("a.b.c"), new Fact("a.b")));
	}
	
	@Test
	public void testPrefixOfFieldAccessWithExclusion() {
		assertTrue(isPrefixOf(new Fact("a^f"), new Fact("a.g")));
		assertFalse(isPrefixOf(new Fact("a.g"), new Fact("a^f")));
	}
	
	@Test
	public void testIdentityWithExclusion() {
		assertTrue(isPrefixOf(new Fact("a^f"), new Fact("a^f")));
		assertTrue(isPrefixOf(new Fact("a^f,g"), new Fact("a^f,g")));
	}
	
	@Test
	public void testDifferentExclusions() {
		assertFalse(isPrefixOf(new Fact("a^f"), new Fact("a^g")));
	}
	
	@Test
	public void testMixedFieldAccess() {
		assertTrue(isPrefixOf(new Fact("a^f"), new Fact("a.g.g")));
		assertFalse(isPrefixOf(new Fact("a^f"), new Fact("a.f.h")));
		assertTrue(isPrefixOf(new Fact("a.f"), new Fact("a.f^g")));
	}
	
	@Test
	public void testMultipleExclusions() {
		assertTrue(isPrefixOf(new Fact("a^f,g"), new Fact("a^f")));
		assertTrue(isPrefixOf(new Fact("a^f,g"), new Fact("a^g")));
		assertFalse(isPrefixOf(new Fact("a^f"), new Fact("a^f,g")));
	}

	@Test
	public void testDifferentAccessPathLength() {
		assertTrue(isPrefixOf(new Fact("a^f"), new Fact("a.g.h")));
	}
	
	@Test
	public void testExclusionRequiresFieldAccess() {
		assertTrue(isPrefixOf(new Fact("a"), new Fact("a^f")));
		assertFalse(isPrefixOf(new Fact("a^f"), new Fact("a")));
		
		assertTrue(isPrefixOf(new Fact("a.f"), new Fact("a.f^g")));
		assertFalse(isPrefixOf(new Fact("a.f^g"), new Fact("a.f")));
		
		assertTrue(isPrefixOf(new Fact("a.f"), new Fact("a.f^g^h")));
		assertFalse(isPrefixOf(new Fact("a.f^g^h"), new Fact("a.f")));
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
