package de.ishuo.sourcepainter;

/**
 * The class to store arrow info including its path and start, end positions
 * 
 * @author shuo
 *
 */
public class SPArrow {
	// Absolute file path of the arrow
	String path;
	
	// Lines and columns are 0-based
	int startL;
	int startC;
	int endL;
	int endC;
	
	
	/**
	 * Constructs an arrow with the given file path, start and end positions
	 * 
	 * @param pathToJavaFile file path
	 * @param startLine 0-based
	 * @param startColumn 0-based
	 * @param endLine 0-based
	 * @param endColumn 0-based
	 */
	public SPArrow(String pathToJavaFile, int startLine, int startColumn, int endLine, int endColumn)
	{
		path = pathToJavaFile;
		startL = startLine;
		startC = startColumn;
		endL = endLine;
		endC = endColumn; 
	}
	
	
	/**
	 * Output example: [line 5 col 4 - line 8 col 9], path: xxxxxx
	 */
	public String toString()
	{
		return "[line " + startL + " col " + startC + " - line " + endL + " col " + endC + "], path: " + path;
	}
	
	
	/**
	 * Two arrows are the same iff their start and end positions are the same and
	 * their paths are the same
	 * 
	 * @param obj another arrow
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} 
		if (obj instanceof SPArrow) {
			SPArrow anotherArrow = (SPArrow) obj;
			return anotherArrow.startL == startL && anotherArrow.startC == startC && 
				anotherArrow.endL == endL && anotherArrow.endC == endC &&
				anotherArrow.path.equals(path);
		}
		return false;
	}

	
}
