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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import heros.alias.AccessPath.PrefixTestResult;

import org.junit.Test;

public class AccessPathUtilTest {

	public static AccessPath<TestFieldRef> ap(String ap) {
		Pattern pattern = Pattern.compile("(\\.|\\^)?([^\\.\\^]+)");
		Matcher matcher = pattern.matcher(ap);
		AccessPath<TestFieldRef> accessPath = new AccessPath<>();
		boolean addedExclusions = false;
		
		while(matcher.find()) {
			String separator = matcher.group(1);
			String identifier = matcher.group(2);
			
			if(".".equals(separator) || separator == null) {
				if(addedExclusions)
					throw new IllegalArgumentException("Access path contains field references after exclusions.");
				accessPath = accessPath.addFieldReference(new TestFieldRef(identifier));
			} else {
				addedExclusions=true;
				String[] excl = identifier.split(",");
				TestFieldRef[] fExcl = new TestFieldRef[excl.length];
				for(int i=0; i<excl.length; i++)
					fExcl[i] = new TestFieldRef(excl[i]);
				accessPath = accessPath.appendExcludedFieldReference(fExcl);
			}
		}
		return accessPath;
	}
	
	@Test
	public void testBaseValuePrefixOfFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, ap("").isPrefixOf(ap("f")));
		assertEquals(NO_PREFIX, ap("f").isPrefixOf(ap("")));
	}
	
	@Test
	public void testBaseValueIdentity() {
		assertEquals(GUARANTEED_PREFIX, ap("").isPrefixOf(ap("")));
	}
	
	@Test
	public void testFieldAccessPrefixOfFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, ap("b").isPrefixOf(ap("b.c")));
		assertEquals(NO_PREFIX, ap("b.c").isPrefixOf(ap("b")));
	}
	
	@Test
	public void testPrefixOfFieldAccessWithExclusion() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("g")));
		assertEquals(NO_PREFIX,ap("g").isPrefixOf(ap("^f")));
	}
	
	@Test
	public void testIdentityWithExclusion() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("^f")));
		assertEquals(GUARANTEED_PREFIX,ap("^f,g").isPrefixOf(ap("^f,g")));
	}
	
	@Test
	public void testDifferentExclusions() {
		assertEquals(POTENTIAL_PREFIX,ap("^f").isPrefixOf(ap("^g")));
	}
	
	@Test
	public void testMixedFieldAccess() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("g.g")));
		assertEquals(NO_PREFIX,ap("^f").isPrefixOf(ap("f.h")));
		assertEquals(GUARANTEED_PREFIX,ap("f").isPrefixOf(ap("f^g")));
	}
	
	@Test
	public void testMultipleExclusions() {
		assertEquals(NO_PREFIX,ap("^f,g").isPrefixOf(ap("^f")));
		assertEquals(POTENTIAL_PREFIX,ap("^f,h").isPrefixOf(ap("^f,g")));
		assertEquals(NO_PREFIX,ap("^f,g").isPrefixOf(ap("^g")));
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("^f,g")));
	}

	@Test
	public void testDifferentAccessPathLength() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("g.h")));
	}
	
	@Test
	public void testExclusionRequiresFieldAccess() {
		assertEquals(GUARANTEED_PREFIX,ap("").isPrefixOf(ap("^f")));
		assertEquals(NO_PREFIX,ap("^f").isPrefixOf(ap("")));
		
		assertEquals(GUARANTEED_PREFIX,ap("f").isPrefixOf(ap("f^g")));
		assertEquals(NO_PREFIX,ap("f^g").isPrefixOf(ap("f")));
		
		assertEquals(GUARANTEED_PREFIX,ap("f").isPrefixOf(ap("f^g^h")));
		assertEquals(NO_PREFIX,ap("f^g^h").isPrefixOf(ap("f")));
	}
	
}
