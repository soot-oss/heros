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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessPathUtilTest {

	@Test
	public void testBaseValuePrefixOfFieldAccess() {
		assertTrue(AccessPathUtil.isPrefixOf(new Fact("a"), new Fact("a.f")));
		assertFalse(AccessPathUtil.isPrefixOf(new Fact("a.f"), new Fact("a")));
	}
	
	@Test
	public void testBaseValueIdentity() {
		assertTrue(AccessPathUtil.isPrefixOf(new Fact("a"), new Fact("a")));
	}
	
	@Test
	public void testFieldAccessPrefixOfFieldAccess() {
		assertTrue(AccessPathUtil.isPrefixOf(new Fact("a.b"), new Fact("a.b.c")));
		assertFalse(AccessPathUtil.isPrefixOf(new Fact("a.b.c"), new Fact("a.b")));
	}
	
	@Test
	public void testAbstractedSummary() {
		assertEquals(new Fact("z.f"), AccessPathUtil.applyAbstractedSummary(new Fact("a.f"), new SummaryEdge<>(new Fact("a"), null, new Fact("z"))));
	}
	
	@Test
	public void testAbstractedFieldAccessSummary() {
		assertEquals(new Fact("z.b.c"), AccessPathUtil.applyAbstractedSummary(new Fact("a.b.c"), new SummaryEdge<>(new Fact("a.b"), null, new Fact("z.b"))));
	}
	
	@Test
	public void testSummaryIntroducesFieldAccess() {
		assertEquals(new Fact("z.b.c"), AccessPathUtil.applyAbstractedSummary(new Fact("a.c"), new SummaryEdge<>(new Fact("a"), null, new Fact("z.b"))));
	}
	
	@Test
	public void testSummaryRemovesFieldAccess() {
		assertEquals(new Fact("z.c"), AccessPathUtil.applyAbstractedSummary(new Fact("a.b.c"), new SummaryEdge<>(new Fact("a.b"), null, new Fact("z"))));
	}
	
	@Test
	public void testNonAbstractedSummary() {
		assertEquals(new Fact("z"), AccessPathUtil.applyAbstractedSummary(new Fact("a"), new SummaryEdge<>(new Fact("a"), null, new Fact("z"))));
	}
	
	@Test
	public void testGeneralizeCallerSourceFact() {
		assertEquals(new Fact("0.f"), AccessPathUtil.generalizeCallerSourceFact(new IncomingEdge<>(new Fact("2"), null, new Fact("0"), null), new Fact("2.f")));
	}
	
	@Test
	public void testGeneralizeCallerSourceFactIdentity() {
		assertEquals(new Fact("0.f"), AccessPathUtil.generalizeCallerSourceFact(new IncomingEdge<>(new Fact("2.f"), null, new Fact("0.f"), null), new Fact("2.f")));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGeneralizeCallerSourceFactNoPrefix() {
		AccessPathUtil.generalizeCallerSourceFact(new IncomingEdge<>(new Fact("2.f"), null, new Fact("0.f"), null), new Fact("2"));
	}
}
