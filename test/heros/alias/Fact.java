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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;


public class Fact implements FieldSensitiveFact<String, FieldRef, Fact> {

	public final String baseValue;
	public final AccessPath<FieldRef> accessPath;
	
	public Fact(String name) {
		Pattern pattern = Pattern.compile("(\\.|\\^)([^\\.\\^]+)");
		Matcher matcher = pattern.matcher(name);
		AccessPath<FieldRef> accessPath = new AccessPath<>();
		boolean addedExclusions = false;
		
		int firstSeparator = matcher.find() ? matcher.start() : name.length();
		baseValue = name.substring(0, firstSeparator);
		matcher.reset();
		
		while(matcher.find()) {
			String separator = matcher.group(1);
			String identifier = matcher.group(2);
			
			if(separator.equals(".")) {
				if(addedExclusions)
					throw new IllegalArgumentException("Access path contains field references after exclusions.");
				accessPath = accessPath.addFieldReference(new FieldRef(identifier));
			} else {
				addedExclusions=true;
				String[] excl = identifier.split(",");
				FieldRef[] fExcl = new FieldRef[excl.length];
				for(int i=0; i<excl.length; i++)
					fExcl[i] = new FieldRef(excl[i]);
				accessPath = accessPath.appendExcludedFieldReference(fExcl);
			}
		}
		this.accessPath = accessPath;
	}
	
	public Fact(String baseValue, AccessPath<FieldRef> accessPath) {
		this.baseValue = baseValue;
		this.accessPath = accessPath;
	}
	
	@Override
	public String toString() {
		return "[Fact "+baseValue+(accessPath.isEmpty() ? "" : accessPath)+"]";
	}

	@Override
	public String getBaseValue() {
		return baseValue;
	}

	@Override
	public AccessPath<FieldRef> getAccessPath() {
		return accessPath;
	}

	@Override
	public void addNeighbor(Fact originalAbstraction) {
		
	}

	@Override
	public void setCallingContext(Fact callingContext) {
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((baseValue == null) ? 0 : baseValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Fact))
			return false;
		Fact other = (Fact) obj;
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		if (baseValue == null) {
			if (other.baseValue != null)
				return false;
		} else if (!baseValue.equals(other.baseValue))
			return false;
		return true;
	}

	@Override
	public Fact cloneWithAccessPath(AccessPath<FieldRef> accessPath) {
		return new Fact(baseValue, accessPath);
	}
	
	
}
