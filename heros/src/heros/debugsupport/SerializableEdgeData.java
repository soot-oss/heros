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

import java.io.Serializable;

public class SerializableEdgeData implements Serializable {

	public static final long serialVersionUID = -6749133467770764139L;
	
	public static enum EdgeKind { JUMP_FUNCTION, EDGE_FUNCTION };
		
	public final String className;
	public final int startLine;
	public final int startColumn;
	public final int endLine;
	public final int endColumn;
	public final String label;
	public final EdgeKind kind;

	public SerializableEdgeData(EdgeKind kind, String className, int startLine, int startColumn, int endLine, int endColumn, String label) {
		this.kind = kind;
		this.className = className;
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
		this.label = label;
	}

	@Override
	public String toString() {
		return "SerializableEdgeData [className=" + className + ", startLine="
				+ startLine + ", startColumn=" + startColumn + ", endLine="
				+ endLine + ", endColumn=" + endColumn + ", label=" + label
				+ "]";
	} 	
	
}
