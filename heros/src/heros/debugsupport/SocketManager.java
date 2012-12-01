package heros.debugsupport;

import heros.EdgeFunction;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketManager {
	
	private static final class NewEdgeSerializer<M, D, N, V> implements NewEdgeListener<M, D, N, V> {
		private final ObjectOutputStream oos;

		public NewEdgeSerializer(ObjectOutputStream oos) {
			this.oos = oos;
		}

		@Override
		public void newJumpFunction(M method, D sourceVal, N target, D targetVal, EdgeFunction<V> f) {
			try {
				oos.writeObject(new JumpFunctionData<M, D, N, V>(method, sourceVal, target, targetVal, f));
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
	}

	public static <M, D, N, V> NewEdgeListener<M, D, N, V> tryGetRemoteEdgeListener() {
		ObjectOutputStream oos = tryConnectToDebugger();
		if(oos==null) return null;		
		return new NewEdgeSerializer<M, D, N, V>(oos);
	}
	
	private static ObjectOutputStream tryConnectToDebugger() {
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
