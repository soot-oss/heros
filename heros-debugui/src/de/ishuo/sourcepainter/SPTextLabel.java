package de.ishuo.sourcepainter;

/**
 * This class stores info in text label
 * 
 * @author shuo
 *
 */
public class SPTextLabel {
	// File path
	String path;
	// Line is 0-based
	int line;
	// Text to be shown in the label
	String text;
	
	/**
	 * Constructs a text label with the given info
	 * 
	 * @param pathToJavaFile file path
	 * @param lineNumber 0-based
	 * @param labelText text to be shown in the label
	 */
	public SPTextLabel(String pathToJavaFile, int lineNumber, String labelText)
	{
		path = pathToJavaFile;
		line = lineNumber;
		text = labelText;
	}
	
	
	/**
	 * Output example: line 5 [info], path: xxxxxx 
	 */
	public String toString()
	{
		return "line " + line + " [" + text + "], path: " + path;
	}

	/**
	 * Two text labels are same iff their texts, lines and paths are the same
	 * 
	 * @param obj another text label
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} 
		if (obj instanceof SPTextLabel) {
			SPTextLabel anotherLabel = (SPTextLabel) obj;
			return anotherLabel.text.equals(text) && anotherLabel.line == line && anotherLabel.path.equals(path);
		}
		return false;
	}

	
}
