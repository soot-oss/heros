package heros.debugui;

import heros.EdgeFunction;
import heros.debugsupport.NewEdgeListener;

public class EdgeDrawing<M, D, N, V> implements NewEdgeListener<M, D, N, V> {

	@Override
	public void newJumpFunction(M method, D sourceData, N target, D targetData, EdgeFunction<V> f) {
		System.err.println(method);
	}

}
