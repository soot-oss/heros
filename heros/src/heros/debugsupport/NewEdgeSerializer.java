/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.debugsupport;

import heros.EdgeFunction;

import java.io.IOException;
import java.io.ObjectOutputStream;

public abstract class NewEdgeSerializer<M,D,N,V> {
	
	protected final ObjectOutputStream oos;

	public NewEdgeSerializer(ObjectOutputStream oos) {
		this.oos = oos;
	}

	public synchronized final void newJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f) {
		SerializableEdgeData data = serializeJumpFunction(method, sourceVal, target, targetVal, f);
		try {
			oos.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public abstract SerializableEdgeData serializeJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f);
	
	public synchronized final void closeConnection() {
		try {
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
