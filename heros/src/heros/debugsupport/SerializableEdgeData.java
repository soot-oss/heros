package heros.debugsupport;

import java.io.Serializable;

public class SerializableEdgeData implements Serializable {

	private static final long serialVersionUID = 833281837577489562L;

	public final String className;

	public SerializableEdgeData(String className) {
		this.className = className;
	} 
	
}
