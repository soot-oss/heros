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

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketManager {
	
	public static ObjectOutputStream tryConnectToDebugger() {
		String hostAndPort = System.getenv("HEROS_DEBUG_PORT");
		if(hostAndPort!=null && !hostAndPort.isEmpty()) {
			//online mode, open Socket
			String[] split = hostAndPort.split(":");
			if(split.length!=2) {
				System.err.println("Expected format: hostname:socket");
				System.exit(1);
			}
			String host = split[0];
			int port = Integer.parseInt(split[1]);
			Socket socket;
			try {
				socket = new Socket(host, port);
				OutputStream os = socket.getOutputStream();
				return new ObjectOutputStream(os);
			} catch (Exception e) {
				e.printStackTrace();
			} 		
		} 
		return null;
	}

}
