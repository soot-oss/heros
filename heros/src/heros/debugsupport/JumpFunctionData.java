package heros.debugsupport;

import heros.EdgeFunction;

import java.io.Serializable;

public class JumpFunctionData<M, D, N, V> implements Serializable {

	private static final long serialVersionUID = -5278071739815468435L;

	public final M method;
	public final D sourceVal;
	public final N target;
	public final D targetVal;
	public final EdgeFunction<V> f;

	public JumpFunctionData(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f) {
		this.method = method;
		this.sourceVal = sourceVal;
		this.target = target;
		this.targetVal = targetVal;
		this.f = f;
	}
	
}
