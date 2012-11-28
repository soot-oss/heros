package de.bodden.ide.flowfunc;

import java.util.Collections;
import java.util.Set;

import de.bodden.ide.FlowFunction;


public class Identity<D> implements FlowFunction<D> {
	
	@SuppressWarnings("rawtypes")
	private final static Identity instance = new Identity();
	
	private Identity(){} //use v() instead

	public Set<D> computeTargets(D source) {
		return Collections.singleton(source);
	}

	@SuppressWarnings("unchecked")
	public static <D> Identity<D> v() {
		return instance;
	}

}
