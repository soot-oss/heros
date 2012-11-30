package de.ishuo.sourcepainter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Label;

/**
 * The central registry for all drawing elements.
 * 
 * @author shuo
 */
public class SourcePainterRegistry {
	
	// Maps to store drawing elements
	private static Map<String, List<SPArrow>> arrowsMap = new HashMap<String, List<SPArrow>>();
	private static Map<String, List<SPTextLabel>> textLabelsMap = new HashMap<String, List<SPTextLabel>>();
	private static Map<String, List<SPContextMenu>> contextMenusMap = new HashMap<String, List<SPContextMenu>>();
	private static Map<Integer, Label> contextMenuLabelsMap = new HashMap<Integer, Label>();
	private static Map<Integer, Label> hoversMap = new HashMap<Integer, Label>();
	private static Map<Integer, ImageData> hoverImagesMap = new HashMap<Integer, ImageData>();
	
	// Used to prevent saving duplicates of the same ContextMenu inside contextMenusMap
	private static Set<Integer> contextMenuShadowIds = new HashSet<Integer>();
	
	// Trashes of drawing elements
	private static List<SPArrow> trashedArrows = new ArrayList<SPArrow>();
	private static List<SPTextLabel> trashedLabels = new ArrayList<SPTextLabel>();
	private static List<SPContextMenu> trashedContextMenus = new ArrayList<SPContextMenu>();
	private static Stack<Label> trashedHovers = new Stack<Label>();
	
	// Delegate to Editor
	static PaintableEditor delegate;
	
	// File path map <title, fullpath>
	private static Map<String, String> filePathsMap = new HashMap<String, String>();
	
	
	///////////////////////////////////////
	//////          Arrows         ////////
	///////////////////////////////////////
	
	/**
	 * Creates a new arrow, adds it to the set of current arrows, let delegate redraw
	 * and return the arrow
	 * 
	 * @param path path to the file in which the arrow resides
	 * @param startLine start line in code of the arrow
	 * @param startCol start column in code of the arrow
	 * @param endLine end line in code of the arrow
	 * @param endCol end column in code of the arrow
	 */
	public static SPArrow registerArrow(String path, int startLine, int startCol, int endLine, int endCol)
	{ 
		if (path == null || path.isEmpty() ||
			startLine < 0 || startCol < 0 || endLine < 0 || endCol < 0)
			return null;
		
		// Create a new arrow
		SPArrow arrow = new SPArrow(path, startLine, startCol, endLine, endCol);
		
		// Create map entry for this file path if it doesn't exist yet
		if (!arrowsMap.containsKey(path)) {
			arrowsMap.put(path, new ArrayList<SPArrow>());
		}
		
		// Add the new arrow to its list if it hasn't been in it
		List<SPArrow> arrows = arrowsMap.get(path);
		if (!arrows.contains(arrow))
			arrows.add(arrow);
		else
			System.out.println("Duplicated arrow found. Not added.");
				
		// Let delegate redraw
		delegate.redraw();
		
		return arrow;
	}
	
	
	/**
	 * Checks whether arrow exists for the given file path
	 * 
	 * @param path file path
	 * @return true if arrow exists for the given path, false if doesn't
	 */
	static boolean hasArrowsOfFile(String path)
	{
		return arrowsMap.containsKey(path);
	}

	
	/**
	 * Returns the list of arrows for the given file path
	 * 
	 * @param path file path
	 * @return the list of arrows for the given path
	 */
	static List<SPArrow> getArrowsOfFile(String path)
	{
		return arrowsMap.get(path);
	}

	
	/**
	 * Removes the given arrow of the given file path
	 * 
	 * @param arrow
	 * @param path
	 * @return true if the removal is successful, false otherwise
	 */
	public static boolean removeArrowOfFile(SPArrow arrow, String path)
	{
		return arrowsMap.get(path).remove(arrow);
	}
	
	
	/**
	 * Puts the given arrow in trash. (For later removal)
	 * 
	 * @param arrow the arrow to be removed
	 */
	public static void trashArrow(SPArrow arrow) 
	{
		trashedArrows.add(arrow);
	}
	
	
	/**
	 * Removes all arrows in the trash and clear trash
	 */
	public static void emptyTrashOfArrows()
	{
		for (SPArrow arrow : trashedArrows) {
			if (!removeArrowOfFile(arrow, arrow.path))
				System.err.println("Cannot empty arrow trash");
			
		}
		trashedArrows.clear();
	}
	
	
	///////////////////////////////////////
	//////        TextLabels       ////////
	///////////////////////////////////////
	
	
	/**
	 * Creates a new text label, adds it to the set of current labels, 
	 * and returns the label
	 * 
	 * @param path path to the file in which the arrow resides
	 * @param lineNumber line number of the label
	 * @param labelText text to be shown in the label
	 * 
	 */
	public static SPTextLabel registerTextLabel(String path, int lineNumber, String labelText)
	{
		if (path == null || path.isEmpty() ||
			lineNumber < 0 || labelText == null || labelText.isEmpty())
			return null;
		
		// Create a new text label
		SPTextLabel label = new SPTextLabel(path, lineNumber, labelText);
		
		// Create map entry for this file path if it doens't exist yet
		if (!textLabelsMap.containsKey(path)) {
			textLabelsMap.put(path, new ArrayList<SPTextLabel>());
		}

		// Add the new text label to its list if it hasn't been in it
		List<SPTextLabel> labels = textLabelsMap.get(path);
		if (!labels.contains(label)) {
			labels.add(label);
		} else
			System.out.println("Duplicated label found. Not added.");
		
		return label;
	}
	
	
	/**
	 * Checks whether text label exists for the given file path
	 * 
	 * @param path file path
	 * @return true if text label exists for the given path, false otherwise
	 */
	static boolean hasTextLabelsOfFile(String path)
	{
		return textLabelsMap.containsKey(path);
	}
	
	
	/**
	 * Returns the list of text labels for the given file path
	 * 
	 * @param path file path
	 * @return the list of text labels for the given path
	 */
	static List<SPTextLabel> getTextLabelsOfFile(String path)
	{
		return textLabelsMap.get(path);
	}
	
	
	/**
	 * Removes the given text label of the given file path
	 * 
	 * @param label
	 * @param path
	 * @return true if the removal is successful, false otherwise
	 */
	public static boolean removeTextLabelOfFile(SPTextLabel label, String path)
	{
		return textLabelsMap.get(path).remove(label);
	}
	
	
	/**
	 * Puts the given text label in trash (For later removal)
	 * 
	 * @param label the arrow to be removed
	 */
	public static void trashTextLabel(SPTextLabel label)
	{
		trashedLabels.add(label);
	}
	
	
	/**
	 * Removes all text labels in the trash and clears trash
	 */
	public static void emptyTrashOfTextLabels()
	{
		for (SPTextLabel label : trashedLabels) {
			if (!removeTextLabelOfFile(label, label.path))
				System.err.println("Cannot empty label trash");
		}
		trashedLabels.clear();
	}
	

	///////////////////////////////////////
	//////          Hovers         ////////
	///////////////////////////////////////
	
	// Hover = SWT Label
	// HoverImage = ImageData
	
	/**
	 * Adds the given hover label to the set of current labels
	 * 
	 * @param shadowId shadow ID of the transition
	 */
	static void registerHover(int shadowId, Label label)
	{
		hoversMap.put(shadowId, label);
	}
	
	
	/**
	 * Checks whether hover with the given shadow ID exists
	 * 
	 * @param shadowId shadow ID of the transition
	 * @return true if hover with the given ID exists, false otherwise
	 */
	static boolean hasHover(int shadowId)
	{
		return hoversMap.containsKey(shadowId);
	}
	
	
	/**
	 * Returns the hover with the given shadow ID
	 * 
	 * @param shadowId shadow ID of the transition
	 * @return the hover with the given ID
	 */
	static Label getHover(int shadowId)
	{
		return hoversMap.get(shadowId);
	}
	
	
	/**
	 * Removes hover with the given shadow ID from hoversMap
	 * and puts it in trash stack (for later reuse or removal)
	 * 
	 * @param shadowId shadow ID of the transition
	 */
	static void trashHover(int shadowId)
	{
		Label removed = hoversMap.remove(shadowId);
		if (removed != null) trashedHovers.push(removed);
	}
	
	/**
	 * Removes all hover within the file denoted by the given 
	 * filename from hoversMap and puts them in trash stack
	 * (for later reuse or removal)
	 * 
	 * @param filename all hovers within this file should be removed
	 */
	public static void trashAllHoversOfFile(String filename)
	{
		if (contextMenusMap.containsKey(filename)) {
			for (SPContextMenu menu : contextMenusMap.get(filename)) {
				int id = menu.shadowId;
				if (hasHover(id)) trashHover(id);
			}
		}
	}
	
	
	/**
	 * Checks whether there is still trashed hover
	 * 
	 * @return true if there is trashed hover, false otherwise
	 */
	static boolean hasTrashedHover()
	{
		return !trashedHovers.isEmpty();
	}
	
	
	/**
	 * Returns the last trashed hover in stack
	 * 
	 * @return the last trashed hover
	 */
	static Label getNextTrashedHover()
	{
		return trashedHovers.pop();
	}
	
	
	/**
	 * Removes and disposes of all hovers in trash stack
	 */
	static void emptyTrashOfHovers()
	{
		while (!trashedHovers.empty()) {
			trashedHovers.pop().dispose();
		}
	}

	
	///////////////////////////////////////
	//////       HoverImages       ////////
	///////////////////////////////////////
	
	/**
	 * Adds the given image to the set of current images
	 * 
	 * @param shadowId the shadow ID of the transition, to which the image binds
	 */
	public static void registerHoverImage(int shadowId, ImageData image)
	{
		hoverImagesMap.put(shadowId, image);
	}
	
	
	/**
	 * Checks whether the image with the given ID exists
	 * 
	 * @param shadowId the shadow ID of the transition
	 * @return true if the image with the given ID exists, false otherwise
	 */
	static boolean hasHoverImage(int shadowId)
	{
		return hoverImagesMap.containsKey(shadowId);
	}
	
	
	/**
	 * Returns the image with the given shadow ID
	 * 
	 * @param shadowId the shadow ID of the transition
	 * @return the image with the given shadow ID
	 */
	public static ImageData getHoverImage(int shadowId)
	{
		return hoverImagesMap.get(shadowId);
	}
	
	
	/**
	 * Removes the image with the given ID
	 * 
	 * @param shadowId the shadow ID of the transition
	 */
	public static void removeHoverImage(int shadowId)
	{
		hoverImagesMap.remove(shadowId);
	}
	
	
	///////////////////////////////////////
	//////      SPContextMenus     ////////
	///////////////////////////////////////
	
	// ContextMenu = SPContextMenu
	// ContextMenuLabel = SWT Label with menu
	
	/**
	 * Creates a new SPContextMenu, adds it to the set of current SPContextMenus
	 * and lets delegate redraw
	 */
	public static void registerContextMenu(String thePath, int theShadowId, int line, int column, int endColumn, SortedMap<String, SortedMap<String, Integer>> theTransitions)
	{
		if (thePath == null || thePath.isEmpty() || contextMenuShadowIds.contains(theShadowId))
			return;

		// Create a new SPContextMenu
		SPContextMenu menu = new SPContextMenu(thePath, theShadowId, line, column, endColumn, theTransitions);

		// Create map entry for this file path if it doesn't exist yet
		if (!contextMenusMap.containsKey(thePath)) {
			contextMenusMap.put(thePath, new ArrayList<SPContextMenu>());
		}

		// Add the SPContextMenu to its list and add its ID to shadow ID map
		// to prevent duplicate creation of SPContextMenus with the same ID
		contextMenusMap.get(thePath).add(menu);
		contextMenuShadowIds.add(theShadowId);
		
		delegate.redraw();
	}
	
	
	/**
	 * Checks whether SPContextMenu exists for the given file path
	 * 
	 * @param path file path
	 * @return true if SPContextMenu exists for the given path
	 */
	static boolean hasContextMenusOfFile(String path)
	{
		return contextMenusMap.get(path) != null;
	}
	
	
	/**
	 * Returns the list of SPContextMenus for the given file path
	 * 
	 * @param path file path
	 * @return the list of SPContextMenus for the given path
	 */
	static List<SPContextMenu> getContextMenusOfFile(String path)
	{
		return contextMenusMap.get(path);
	}
	
	
	/**
	 * Puts the given SPContextMenu in trash.
	 * 
	 * @param menu the SPContextMenu to be removed
	 */
	public static void trashContextMenu(SPContextMenu menu) 
	{
		trashedContextMenus.add(menu);
	}
	

	/**
	 * Removes all SPContextMenus in the trash and their IDs
	 * in shadow ID map
	 */
	public static void emptyTrashOfContextMenus()
	{
		for (SPContextMenu aTrashedMenu : trashedContextMenus) {
			int id = aTrashedMenu.shadowId;
			String path = aTrashedMenu.path;
			
			contextMenuShadowIds.remove(id);
			
			List<SPContextMenu> menus = contextMenusMap.get(path);
			Iterator<SPContextMenu> iterator = menus.iterator();
			while (iterator.hasNext()) {
				SPContextMenu menu = iterator.next();
				if (menu.shadowId == id)
					iterator.remove();
			}
		}
		trashedContextMenus.clear();
	}
	

	///////////////////////////////////////
	//////    ContextMenuLabels    ////////
	///////////////////////////////////////
	
	/**
	 * Adds the context menu label to the set of current labels
	 */
	static void registerContextMenuLabel(int shadowId, Label label)
	{
		contextMenuLabelsMap.put(shadowId, label);
	}
	
	/**
	 * Checks whether context menu label with the given id exists
	 * 
	 * @param shadowId the shadow ID of the transition
	 * @return true if the context menu label with the given id exists, false otherwise
	 */
	static boolean hasContextMenuLabel(int shadowId)
	{
		return contextMenuLabelsMap.containsKey(shadowId);
	}
	
	
	/**
	 * Dispose of the context menu label with the given id
	 * of the given file and removes it from the map
	 * 
	 * @param id the shadow ID of the transition
	 * @param filename absolute file path
	 */
	static void removeContextMenuLabelOfFile(int id, String filename)
	{
		contextMenuLabelsMap.get(id).dispose();
		contextMenuLabelsMap.remove(id);
	}

	
	/**
	 * Dispose of all context menu labels within the given file
	 * and removes them from the map. Internally calls
	 * {@link #removeContextMenuLabelOfFile(int, String)}
	 * 
	 * @param filename absolute file path
	 */
	public static void removeAllContextMenuLabelsOfFile(String filename)
	{
		if (contextMenusMap.containsKey(filename)) {
			for (SPContextMenu menu : contextMenusMap.get(filename)) {
				int id = menu.shadowId;
				removeContextMenuLabelOfFile(id, filename);
			}
		}
	}

	///////////////////////////////////////
	//////      Miscellaneous      ////////
	///////////////////////////////////////

	/**
	 * Register file name in a key-value pair, where the name
	 * is the key and its full OS path is the value
	 * 
	 * @param fileOSPath full OS path
	 */
	public static void registerFilePath(String fileOSPath) 
	{
		File file = new File(fileOSPath);
		filePathsMap.put(file.getName(), fileOSPath);
	}
	
	
	/**
	 * Get the full OS path with its file name
	 * 
	 * @param filename file name
	 * @return the full OS path to this file
	 */
	public static String getOSPath(String filename)
	{
		return filePathsMap.get(filename);
	}
}
