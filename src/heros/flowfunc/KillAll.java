package heros.flowfunc;

import heros.FlowFunction;

import java.util.Collections;
import java.util.Set;



/**
 * The empty function, i.e. a function which returns an empty set for all points
 * in the definition space.
 *  
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public class KillAll<D> implements FlowFunction<D> {
	
	@SuppressWarnings("rawtypes")
	private final static KillAll instance = new KillAll();
	
	private KillAll(){} //use v() instead

	public Set<D> computeTargets(D source) {
		return Collections.emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public static <D> KillAll<D> v() {
		return instance;
	}

}
