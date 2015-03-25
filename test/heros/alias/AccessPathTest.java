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

import static org.junit.Assert.*;


import org.junit.Test;

import com.google.common.collect.Sets;

import heros.alias.AccessPath.PrefixTestResult;

@SuppressWarnings("unchecked")
public class AccessPathTest {

	
	private static AccessPath<String> ap(String... path) {
		return new AccessPath<String>(path, Sets.<String> newHashSet());
	}
	
	@Test
	public void append() {
		AccessPath<String> sut = ap("a");
		assertEquals(ap("a", "b"), sut.append("b"));
	}
	
	@Test
	public void addOnExclusion() {
		AccessPath<String> sut = ap().appendExcludedFieldReference("a");
		assertEquals(ap("b"), sut.append("b"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void addMergedFieldsOnSingleExclusion() {
		AccessPath<String> sut = ap().appendExcludedFieldReference("a");
		sut.append("a");	
	}
	
	@Test
	public void prepend() {
		assertEquals(ap("c", "a", "b"), ap("a", "b").prepend("c"));
	}
	
	@Test
	public void remove() {
		assertEquals(ap("b"), ap("a", "b").removeFirst());
	}
	
	@Test
	public void deltaDepth1() {
		String[] actual = ap("a").getDeltaTo(ap("a", "b")).accesses;
		assertArrayEquals(new String[] { "b" }, actual);
	}
	
	@Test
	public void deltaDepth2() {
		String[] actual = ap("a").getDeltaTo(ap("a", "b", "c")).accesses;
		assertArrayEquals(new String[] { "b", "c" }, actual);
	}
	
	@Test
	public void emptyDeltaOnEqualExclusions() {
		AccessPath<String> actual = ap().appendExcludedFieldReference("f");
		assertEquals(0, actual.getDeltaTo(ap().appendExcludedFieldReference("f")).accesses.length);
		assertTrue(actual.getDeltaTo(ap().appendExcludedFieldReference("f")).exclusions.equals(Sets.newHashSet("f")));
	}
	
	@Test
	public void multipleExclPrefixOfMultipleExcl() {
		AccessPath<String> sut = ap().appendExcludedFieldReference("f", "g");
		assertEquals(PrefixTestResult.POTENTIAL_PREFIX, sut.isPrefixOf(ap().appendExcludedFieldReference("f", "h")));
	}
}
