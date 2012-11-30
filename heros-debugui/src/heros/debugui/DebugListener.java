package heros.debugui;

import heros.EdgeFunction;
import heros.debugsupport.NewEdgeListener;

public class DebugListener<M, D, N, V> implements NewEdgeListener<M, D, N, V> {

	@Override
	public void newJumpFunction(M method, D srcValue, N tgt, D tgtValue, EdgeFunction<V> f) {
		System.err.println("soso");
	}

}
