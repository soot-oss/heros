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


import java.util.Arrays;

import com.google.common.base.Joiner;


public class Fact implements FieldSensitiveFact<String, Fact> {

	public final String baseValue;
	public final FieldReference[] accessPath;
	
	public Fact(String name) {
		String[] split = name.split("\\.");
		baseValue = split[0];
		accessPath = new FieldReference[split.length-1];
		for (int i = 1; i < split.length; i++) {
			accessPath[i-1] = new FieldReference.SpecificFieldReference(split[i]);
		}
	}
	
	public Fact(String baseValue, FieldReference[] accessPath) {
		this.baseValue = baseValue;
		this.accessPath = accessPath;
	}

	
	@Override
	public String toString() {
		return "[Fact "+baseValue+(accessPath.length>0 ? "."+Joiner.on(".").join(accessPath) : "" )+"]";
	}

	@Override
	public String getBaseValue() {
		return baseValue;
	}

	@Override
	public FieldReference[] getAccessPath() {
		return accessPath;
	}


	@Override
	public void addNeighbor(FieldSensitiveFact<String, Fact> originalAbstraction) {
		
	}


	@Override
	public void setCallingContext(FieldSensitiveFact<String, Fact> callingContext) {
		
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(accessPath);
		result = prime * result
				+ ((baseValue == null) ? 0 : baseValue.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Fact other = (Fact) obj;
		if (!Arrays.equals(accessPath, other.accessPath))
			return false;
		if (baseValue == null) {
			if (other.baseValue != null)
				return false;
		} else if (!baseValue.equals(other.baseValue))
			return false;
		return true;
	}




	@Override
	public Fact cloneWithAccessPath(FieldReference... accessPath) {
		return new Fact(baseValue, accessPath);
	}
	
	
}
