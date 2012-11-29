package heros.flowfunc;

import heros.FlowFunction;

import java.util.Collections;
import java.util.Set;



/**
 * Function that kills a specific value (i.e. returns an empty set for when given this
 * value as an argument), but behaves like the identity function for all other values.
 *
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public class Kill<D> implements FlowFunction<D> {
	
	private final D killValue;
	
	public Kill(D killValue){
		this.killValue = killValue;
	} 

	public Set<D> computeTargets(D source) {
		if(source==killValue) {
			return Collections.emptySet();
		} else
			return Collections.singleton(source);
	}
	
}
