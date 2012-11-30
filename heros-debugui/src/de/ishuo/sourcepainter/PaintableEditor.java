package de.ishuo.sourcepainter;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


/**
 * Extended Java Editor that supports arrows and labels which 
 * represent transitions between states of objects. It also supports
 * jumping to related transitions in other methods or other files
 * via context menu on each state.
 * 
 * @author shuo
 *
 */
@SuppressWarnings("restriction")
public class PaintableEditor extends CompilationUnitEditor {
	// The OS-specific path representation of the current java file
	String fileOSPath;
	StyledText styledText;

	// Stores number of newlines added or removed, shared
	// between Listeners
	int numNewLine = -1;
	int numDeletedLine = 0;
	
	
	/**
	 * The added drawing part.
	 * 
	 * @see AbstractTextEditor#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {

		super.createPartControl(parent);

		// Testcases
//		SourcePainterRegistry.registerLabel("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/IntraFlowSens1.java", 5, "Test message");
//		SourcePainterRegistry.registerLabel("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/AbstractTest.java", 4, "Test message 2");
//
//		SourcePainterRegistry.registerArrow("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/IntraFlowSens1.java", 3, 10, 5, 2);
//		SourcePainterRegistry.registerArrow("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/AB.java", 2, 10, 4, 2);
//		SourcePainterRegistry.registerArrow("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/IntraFlowSens1.java", 9, 2, 13, 10);
//		SourcePainterRegistry.registerArrow("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/AbstractTest.java", 6, 2, 6, 34);
//		SourcePainterRegistry.registerArrow("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/abc.java", 21, 8, 17, 2);
//
//		SourcePainterRegistry.registerArrow("/Users/shuo/Documents/Programming/Java/WS 0910/clara.vis/test-workspace/Clara test/src/IntraFlowSens1.java", 5, 2, 9, 2);

		
		// The actual drawing takes place in ArrowLabelPaintListener
		styledText.addPaintListener(new ArrowLabelPaintListener(this, styledText));

		// Refresh if user scrolls horizontally
		styledText.getHorizontalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				styledText.redraw();
			}
		});

		styledText.addExtendedModifyListener(new ExtendedModifyListener() {

			@Override
			public void modifyText(ExtendedModifyEvent event) {
				styledText.redraw();

			}
		});


		// Highlights the lines with text label
		styledText.addLineBackgroundListener(new LineBackgroundListener() {

			@Override
			public void lineGetBackground(LineBackgroundEvent event) {
				if (!SourcePainterRegistry.hasTextLabelsOfFile(fileOSPath)) return;

				int lineInQuestion = styledText.getLineAtOffset(event.lineOffset);
				for (SPTextLabel label : SourcePainterRegistry.getTextLabelsOfFile(fileOSPath)) {
					if (label.line == lineInQuestion)
						event.lineBackground = Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
				}

			}
		});

		// Detect text modification. Remove and modify drawing elements accordingly
		final IDocument doc = getDocumentProvider().getDocument(getEditorInput());
		doc.addDocumentListener(new IDocumentListener() {

			@Override
			// Calculate the newline difference
			public void documentAboutToBeChanged(DocumentEvent event) {
			
				numNewLine = 0;
			
				// Calculate how many newlines are deleted
				if (event.fLength > 0) {
					try {
						String deleted = doc.get(event.fOffset, event.fLength);
						numNewLine -= countNewlines(deleted);
						numDeletedLine = -numNewLine;
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
				// Calculate how many newlines are added
				if (event.fText.length() > 0) {
					numNewLine += countNewlines(event.fText);
				}
			}

			@Override
			public void documentChanged(DocumentEvent event) {

				// Get in which line did the event occur
				int modifiedLine = 0;
				try {
					modifiedLine = doc.getLineOfOffset(event.fOffset);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}

				// Adjust drawing elements
				adjustElements(numNewLine, modifiedLine, modifiedLine + numDeletedLine);

				// Reset numNewLine
				numNewLine = 0;
			}
		});


	}


	/**
	 * Adjust drawing elements
	 * 
	 * @param numNewLine difference of newlines after the edit event
	 * @param fromLine first line involved in edit event
	 * @param toLine last line involved in edit event
	 */
	private void adjustElements(int numNewLine, int fromLine, int toLine) {
		// Create set of modified line interval
		HashSet<Integer> interval = new HashSet<Integer>();
		for (int i = fromLine; i <= toLine; i++) {
			interval.add(i);
		}
		
		// Handle SPArrow
		if (SourcePainterRegistry.hasArrowsOfFile(fileOSPath)) {
	
			// If input occurred in range of an arrow it is removed (interpreted as
			// error correction of user). Relevant arrows are shifted if newline
			// got added or removed
			for (SPArrow arrow : SourcePainterRegistry.getArrowsOfFile(fileOSPath)) {
				// both startL and modifiedLine are 0-based
				if (numNewLine != 0 && arrow.startL > toLine) {
					arrow.startL += numNewLine;
					arrow.endL += numNewLine;
	
				} else {
					for (int i = arrow.startL; i <= arrow.endL; i++) {
						if (interval.contains(i)) {
							SourcePainterRegistry.trashArrow(arrow);
							break;
						}
					}
				}
			}
			SourcePainterRegistry.emptyTrashOfArrows();
		}
	
		// Handle SPLabel
		if (SourcePainterRegistry.hasTextLabelsOfFile(fileOSPath)) {
	
			// If input occurred in the same line of an label it is removed 
			// (interpreted as error correction of user). Relevant labels are shifted 
			// if newline got added or removed
			for (SPTextLabel label : SourcePainterRegistry.getTextLabelsOfFile(fileOSPath)) {
	
				if (interval.contains(label.line)) {
					SourcePainterRegistry.trashTextLabel(label);
				} else if (numNewLine != 0 && label.line > toLine) {
					label.line += numNewLine;
				}
			}
			SourcePainterRegistry.emptyTrashOfTextLabels();
			
		}
	
		// Handle SPContextMenus, ContextMenuLabels and Hovers
		if (SourcePainterRegistry.hasContextMenusOfFile(fileOSPath)) {
			
			// If newlines are added or removed, relevant SPContextMenu are updated
			// and all ContextMenuLabels and Hovers are removed or recycled and 
			// in the next draw event they will be redrawn in new positions. 
			// If input occurred in the same line of them they are removed.
			if (numNewLine != 0) {
				SourcePainterRegistry.removeAllContextMenuLabelsOfFile(fileOSPath);
				SourcePainterRegistry.trashAllHoversOfFile(fileOSPath);
				
				for (SPContextMenu menu : SourcePainterRegistry.getContextMenusOfFile(fileOSPath)) {
					if (interval.contains(menu.line)) {
						SourcePainterRegistry.trashContextMenu(menu);
					} else if (menu.line > toLine) {
						menu.line += numNewLine;
					}
				}
			} else {
				for (SPContextMenu menu : SourcePainterRegistry.getContextMenusOfFile(fileOSPath)) {
					if (interval.contains(menu.line)) {
						int id = menu.shadowId;
						SourcePainterRegistry.removeContextMenuLabelOfFile(id, fileOSPath);
						// Recycle Hover
						SourcePainterRegistry.trashHover(id);
						SourcePainterRegistry.removeHoverImage(id);
						// Remove SPContextMenu
						SourcePainterRegistry.trashContextMenu(menu);
					}
				}
			}
			SourcePainterRegistry.emptyTrashOfContextMenus();
			
		}
	}

	
	/**
	 * Get number of newline symbols in text
	 * 
	 * @param text
	 * @return number of newline symbols
	 */
	int countNewlines(String text)
	{
		int count = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n')
				count++;
		}
		
		return count;
	}


	/**
	 * Jump the caret to the given line of current file in this editor instance.
	 * Redraw all transition labels if a scroll occurs during the jump.
	 * 
	 * @param lineNumber the 1-based destination line number
	 */
	void jumpToLine(int lineNumber) {
		// Compute which offset to jump to (first non-empty letter of the line)
		// getOffsetAtLine: line number 0-based
		int offset = styledText.getOffsetAtLine(lineNumber - 1);
		String line = styledText.getTextRange(offset, 10);

		// Excluding preceding spaces in offset
		int i = 0;
		while (line.charAt(i) == ' ' || line.charAt(i) == '\t') {
			offset++;
			i++;
		}

		// Get current top and bottom line numbers of the editor
		int topLineIndex = styledText.getTopIndex() + 1;
		int bottomLineIndex = JFaceTextUtil.getPartialBottomIndex(styledText) + 1;

		styledText.setSelection(offset);
		
		// If page scroll occurs labels must be manually redrawn
		if (lineNumber < topLineIndex || lineNumber > bottomLineIndex) {
			SourcePainterRegistry.removeAllContextMenuLabelsOfFile(fileOSPath);
			SourcePainterRegistry.trashAllHoversOfFile(fileOSPath);
			// DEBUG
			System.out.println("refreshed");
		}
	}
	 

	@Override
	/**
	 * Dispose of remaining SWT labels
	 */
	public void dispose() {
		super.dispose();

		// Dispose of SWT labels (ContextMenuLabel and its Hovers)
		SourcePainterRegistry.emptyTrashOfHovers();			
		
		SourcePainterRegistry.removeAllContextMenuLabelsOfFile(fileOSPath);
		
	}

	
	/**
	 * The method called after initialization. 
	 */
	@Override
	public void aboutToBeReconciled() {
		super.aboutToBeReconciled();

		styledText = getSourceViewer().getTextWidget();
		
		SourcePainterRegistry.delegate = this;
		
		// Get full OS path
		CompilationUnit viewPartInput = (CompilationUnit)getViewPartInput();
		IPath path = viewPartInput.getPath();
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		fileOSPath = resource.getLocation().toOSString();
		
		// Register full OS path in a Key-Value pair, where key 
		// is the file name and value is this full OS path
		SourcePainterRegistry.registerFilePath(fileOSPath);
	}


	void redraw() {
		styledText.redraw();
	}
}
