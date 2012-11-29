package de.bodden.ide.flowfunc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.bodden.ide.FlowFunction;


public class Transfer<D> implements FlowFunction<D> {
	
	private final D toValue;
	private final D fromValue;
	
	public Transfer(D toValue, D fromValue){
		this.toValue = toValue;
		this.fromValue = fromValue;
	} 

	public Set<D> computeTargets(D source) {
		if(source==fromValue) {
			HashSet<D> res = new HashSet<D>();
			res.add(source);
			res.add(toValue);
			return res;
		} else if(source==toValue) {
			return Collections.emptySet();
		} else {
			return Collections.singleton(source);
		}
	}
	
}
