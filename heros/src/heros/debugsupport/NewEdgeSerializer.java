package heros.debugsupport;

import heros.EdgeFunction;

import java.io.IOException;
import java.io.ObjectOutputStream;

public abstract class NewEdgeSerializer<M,D,N,V> {
	
	protected final ObjectOutputStream oos;

	public NewEdgeSerializer(ObjectOutputStream oos) {
		this.oos = oos;
	}

	public final void newJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f) {
		SerializableEdgeData data = serializeJumpFunction(method, sourceVal, target, targetVal, f);
		try {
			oos.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public abstract SerializableEdgeData serializeJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f);
	
	public final void closeConnection() {
		try {
			oos.close();
		} catch (IOException e) {
		}
	}

}
