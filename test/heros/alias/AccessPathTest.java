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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

import heros.alias.AccessPath.Delta;
import heros.alias.AccessPath.PrefixTestResult;
import heros.alias.SubAccessPath.*;

@SuppressWarnings("unchecked")
public class AccessPathTest {

	private static TestFieldRef f(String s) {
		return new TestFieldRef(s);
	}
	
	private static TestFieldRef[] f(String...s) {
		TestFieldRef[] result = new TestFieldRef[s.length];
		for(int i=0; i<s.length; i++) {
			result[i] = f(s[i]);
		}
		return result;
	}

	private static SetOfPossibleFieldAccesses<TestFieldRef> anyOf(String...fields) {
		Set<TestFieldRef> set = Sets.newHashSet();
		for(String f : fields)
			set.add(new TestFieldRef(f));
		return new SetOfPossibleFieldAccesses<TestFieldRef>(set);
	}
	
	private static SpecificFieldAccess<TestFieldRef> s(String field) {
		return new SpecificFieldAccess<TestFieldRef>(new TestFieldRef(field));
	}
	
	private static AccessPath<TestFieldRef> ap(SubAccessPath<TestFieldRef>... path) {
		return new AccessPath<TestFieldRef>(path, Sets.<TestFieldRef> newHashSet());
	}
	
	@Test
	public void addAndMergeAll() {
		AccessPath<TestFieldRef> sut = AccessPath.<TestFieldRef>empty().addFieldReference(f("a", "b", "c"));
		assertEquals(ap(anyOf("a","b","c")), sut.addFieldReference(f("a")));
	}

	@Test
	public void addAndMergeSuffix() {
		AccessPath<TestFieldRef> sut = AccessPath.<TestFieldRef>empty().addFieldReference(f("a", "b", "c"));
		assertEquals(ap(s("a"), anyOf("b","c")), sut.addFieldReference(f("b")));
	}
	
	@Test
	public void addMultipleFieldsAndMerge() {
		AccessPath<TestFieldRef> sut = AccessPath.<TestFieldRef>empty().addFieldReference(f("a"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(f("b", "a")));
	}
	
	@Test
	public void addWithoutMerge() {
		AccessPath<TestFieldRef> sut = ap(s("a"));
		assertEquals(ap(s("a"), s("b")), sut.addFieldReference(f("b")));
	}
	
	@Test
	public void addMergedFields() {
		AccessPath<TestFieldRef> sut = ap(s("a"));
		assertEquals(ap(anyOf("a")), sut.addFieldReference(anyOf("a")));
	}

	@Test
	public void addMergedFieldsOnExclusion() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("a"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(anyOf("a", "b")));
	}
	
	@Test
	public void addMergedFieldsOnNestedExclusion() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("a"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(anyOf("a", "b")));
	}
	
	@Test
	public void addFieldThatMerges() {
		AccessPath<TestFieldRef> sut = ap(s("a"), s("b")).appendExcludedFieldReference(f("x"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(s("a")));
	}
	
	@Test
	public void addFieldThatMergesResultingInExclusionOfMergedField() {
		AccessPath<TestFieldRef> sut = ap(s("a"), s("b")).appendExcludedFieldReference(f("c")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(s("a")));
	}
	
	@Test
	public void addFieldThatMergesResultingInExclusionOfMergedField2() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "c")), sut.addFieldReference(anyOf("a","c")));
	}
	
	@Test
	public void addFieldThatMergesResultingInExclusionOfMergedField3() {
		AccessPath<TestFieldRef> sut = ap(s("c")).appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("c")), sut.addFieldReference(anyOf("c")));
	}
	
	@Test
	public void addOnExclusion() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("a"));
		assertEquals(ap(s("b")), sut.addFieldReference(s("b")));
	}
	
	@Test
	public void addOnNestedExclusion() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "c")), sut.addFieldReference(anyOf("a", "c")));
	}

	@Test(expected=IllegalArgumentException.class)
	public void addMergedFieldsOnSingleExclusion() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("a"));
		sut.addFieldReference(anyOf("a"));	
	}
	
	@Test
	public void prependWithoutMerge() {
		assertEquals(ap(s("c"), s("a"), s("b")), ap(s("a"), s("b")).prepend(f("c")));
	}
	
	@Test
	public void prependWithMerge() {
		assertEquals(ap(anyOf("a"), anyOf("b", "c")), ap(s("a"), anyOf("b", "c")).prepend(f("a")));
	}
	
	@Test
	public void prependAndMergeWithSet() {
		assertEquals(ap(anyOf("a", "b", "c")), ap(s("a"), anyOf("b", "c")).prepend(f("b")));
	}
	
	@Test
	public void remove() {
		assertEquals(ap(s("b")), ap(s("a"), s("b")).removeFirst(f("a")));
	}
	
	@Test
	public void dontRemoveMergedFields() {
		assertEquals(ap(anyOf("a", "b")), ap(anyOf("a", "b")).removeFirst(f("a")));
	}
	
	@Test
	public void removeMergedFieldsIfRemovingSuffix() {
		assertEquals(ap(), ap(anyOf("a", "b"), s("c")).removeFirst(f("c")));
	}
	
	@Test
	public void deltaDepth1() {
		SubAccessPath<TestFieldRef>[] actual = ap(s("a")).getDeltaTo(ap(s("a"), s("b"))).accesses;
		assertArrayEquals(new SubAccessPath[] { s("b") }, actual);
	}
	
	@Test
	public void deltaDepth2() {
		SubAccessPath<TestFieldRef>[] actual = ap(s("a")).getDeltaTo(ap(s("a"), s("b"), s("c"))).accesses;
		assertArrayEquals(new SubAccessPath[] { s("b"), s("c") }, actual);
	}
	
	@Test
	public void delta() {
		SubAccessPath<TestFieldRef>[] actual = ap(s("a")).getDeltaTo(ap(s("a"), anyOf("b"))).accesses;
		assertArrayEquals(new SubAccessPath[] { anyOf("b") }, actual);
	}
	
	@Test
	public void delta2() {
		SubAccessPath<TestFieldRef>[] actual = ap(s("f"), s("g"), s("h")).getDeltaTo(ap(anyOf("f", "g"), s("h"))).accesses;
		assertArrayEquals(new SubAccessPath[] {  }, actual);
	}
	
	@Test
	public void delta3() {
		SubAccessPath<TestFieldRef>[] actual = ap(s("f"), s("f")).getDeltaTo(ap(anyOf("f"))).accesses;
		assertArrayEquals(new SubAccessPath[] { anyOf("f") } , actual);
	}
	
	@Test
	public void deltaFromSetToSet() {
		Delta<TestFieldRef> actual = ap(anyOf("a")).appendExcludedFieldReference(f("f")).getDeltaTo(ap(anyOf("a")).appendExcludedFieldReference(f("g")));
		assertArrayEquals(new SubAccessPath[] { anyOf("a") }, actual.accesses);
		assertEquals(Sets.newHashSet(f("g")), actual.exclusions);
	}
	
	@Test
	public void emptyDeltaOnEqualExclusions() {
		AccessPath<TestFieldRef> actual = ap().appendExcludedFieldReference(f("f"));
		assertEquals(0, actual.getDeltaTo(ap().appendExcludedFieldReference(f("f"))).accesses.length);
		assertTrue(actual.getDeltaTo(ap().appendExcludedFieldReference(f("f"))).exclusions.equals(Sets.newHashSet(f("f"))));
	}
	
	@Test
	public void deltaMatchingMergedField() {
		SubAccessPath<TestFieldRef>[] actual = ap(s("a"), s("b")).getDeltaTo(ap(s("a"), anyOf("b"))).accesses;
		assertArrayEquals(new SubAccessPath[] { anyOf("b") }, actual);
	}
	
	@Test
	public void prefixOfMergedField() {
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f")).isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f")).isPrefixOf(ap(anyOf("f", "g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f")).isPrefixOf(ap(anyOf("f"), s("h"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f"), s("h")).isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void noPrefixOfMergedField() {
		assertEquals(PrefixTestResult.NO_PREFIX, ap(s("g")).isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.NO_PREFIX, ap(s("g")).isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void prefixOfExclusion() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("f"));
		assertEquals(PrefixTestResult.NO_PREFIX, sut.isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("f", "g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void prefixOfExclusions() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("f", "g"));
		assertEquals(PrefixTestResult.NO_PREFIX, sut.isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.NO_PREFIX, sut.isPrefixOf(ap(anyOf("f", "g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void multipleExclPrefixOfMultipleExcl() {
		AccessPath<TestFieldRef> sut = ap().appendExcludedFieldReference(f("f", "g"));
		assertEquals(PrefixTestResult.POTENTIAL_PREFIX, sut.isPrefixOf(ap().appendExcludedFieldReference(f("f", "h"))));
	}
	
	@Test
	public void mergedFieldsPrefixOf() {
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap(s("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap(s("g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap().appendExcludedFieldReference(f("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap().appendExcludedFieldReference(f("f", "g"))));
		
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f"), s("h")).isPrefixOf(ap(s("f"), s("h"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).appendExcludedFieldReference(f("f")).isPrefixOf(ap(s("f"), s("h"))));
	}

	@Test
	public void subsumes() {
		assertTrue(ap(anyOf("f")).subsumes(ap(s("f"))));
		assertFalse(ap(s("f")).subsumes(ap(anyOf("f"))));
		
		assertTrue(ap(anyOf("f", "g")).subsumes(ap(s("f"), s("g"))));
		assertFalse(ap(s("f"), s("g")).subsumes(ap(anyOf("f", "g"))));
		
		assertTrue(ap(anyOf("f", "g")).subsumes(ap(anyOf("f"), anyOf("g"))));
		assertFalse(ap(anyOf("f"), anyOf("g")).subsumes(ap(anyOf("f", "g"))));
	}

	@Test
	public void subsumesWithExclusions() {
		assertTrue(ap().subsumes(ap().appendExcludedFieldReference(f("a"))));
		assertFalse(ap().appendExcludedFieldReference(f("a")).subsumes(ap()));
		
		assertTrue(ap().appendExcludedFieldReference(f("a")).subsumes(ap().appendExcludedFieldReference(f("a", "b"))));
		assertFalse(ap().appendExcludedFieldReference(f("a", "b")).subsumes(ap().appendExcludedFieldReference(f("a"))));
	}
}
