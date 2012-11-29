package heros.flowfunc;

import heros.FlowFunction;

import java.util.Set;


import com.google.common.collect.Sets;


/**
 * Represents the ordered composition of a set of flow functions.
 */
public class Compose<D> implements FlowFunction<D> {
	
	private final FlowFunction<D>[] funcs;

	public Compose(FlowFunction<D>... funcs){
		this.funcs = funcs;
	} 

	public Set<D> computeTargets(D source) {
		Set<D> curr = Sets.newHashSet();
		curr.add(source);
		for (FlowFunction<D> func : funcs) {
			Set<D> next = Sets.newHashSet();
			for(D d: curr)
				next.addAll(func.computeTargets(d));
			curr = next;
		}
		return curr;
	}
	
}