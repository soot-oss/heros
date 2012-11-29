package heros.flowfunc;

import heros.FlowFunction;

import java.util.Collections;
import java.util.Set;



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
