package heros.debugsupport;

import heros.EdgeFunction;

public interface NewEdgeSerializer<M,D,N,V> {
	
	public SerializableEdgeData newJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f);

}
