package de.ishuo.sourcepainter;

import java.util.SortedMap;

/**
 * The class to store context menu info including related transitions in
 * other methods and files. Actual menu label is stored elsewhere.
 * 
 * @author shuo
 *
 */
public class SPContextMenu {
	// File path
	String path;
	
	// Shadow ID of the transition
	int shadowId;
	
	// Lines and columns are 0-based
	int line;
	int column;
	int endColumn;
	
	// Related transitions <Path, <Trans, targetLine>>
	SortedMap<String, SortedMap<String, Integer>> transitions;
	
	
	/**
	 * Constructs a SPContextMenu with the given info
	 * 
	 * @param thePath absolute file path
	 * @param theShadowId shadow ID of the transition
	 * @param theLine 0-based
	 * @param theColumn 0-based
	 * @param theEndColumn 0-based
	 * @param theTransitions related transitions
	 */
	public SPContextMenu(String thePath, int theShadowId, int theLine, int theColumn, 
			int theEndColumn, SortedMap<String, SortedMap<String, Integer>> theTransitions)
	{
		path = thePath;
		shadowId = theShadowId;
		line = theLine;
		column = theColumn;
		endColumn = theEndColumn;
		transitions = theTransitions;
	}
	

	
	
}
