package heros.debugsupport;

import java.io.Serializable;

public class SerializableEdgeData implements Serializable {

	public static final long serialVersionUID = -6749133467770764139L;
	
	public final String className;
	public final int startLine;
	public final int startColumn;
	public final int endLine;
	public final int endColumn;
	public final String label;

	public SerializableEdgeData(String className, int startLine, int startColumn, int endLine, int endColumn, String label) {
		this.className = className;
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
		this.label = label;
	} 
	
}
