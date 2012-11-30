package heros.debugsupport;

import heros.EdgeFunction;

public interface NewEdgeListener<M,D,N,V> {
	
	public void newJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f);

}
