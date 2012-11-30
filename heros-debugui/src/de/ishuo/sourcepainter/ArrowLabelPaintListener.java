/**
 * 
 */
package de.ishuo.sourcepainter;

import java.io.File;
import java.util.List;
import java.util.SortedMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;


/**
 * This class draws arrows, text labels and context menus
 * according to the data in SourcePainterRegistry
 * 
 * @author shuo
 *
 */
final class ArrowLabelPaintListener implements PaintListener {

	// Constants for clickable visual cue of context menus
	private static final String kRightClickable = "*";
	private static final String kNotRightClickable = "";
	
	private final PaintableEditor paintableEditor;
	private final StyledText styledText;
	private String fileOSPath = "";
	
	/**
	 * Add mouse listeners for showing/hiding visual cues and 
	 * create hover labels
	 * 
	 * @author shuo
	 */
	private final class SPMouseTrackListener implements MouseTrackListener {
		private final Label menuLabel;
		private final int charHeight;
		private final int id;
	
		private SPMouseTrackListener(Label menuLabel, int charHeight, int id) {
			this.menuLabel = menuLabel;
			this.charHeight = charHeight;
			this.id = id;
		}
	
		@Override
		// Hover mouse over a label to show its transition hover label
		// if its image is registered.
		public void mouseHover(MouseEvent e) {						
			if (!SourcePainterRegistry.hasHoverImage(id)) {
				return;
			} else if (SourcePainterRegistry.hasHover(id)) {
				// Set label visible if it is already drawn
				SourcePainterRegistry.getHover(id).setVisible(true);
			} else {
				// Draw new hover label
				ImageData imageData = SourcePainterRegistry.getHoverImage(id);
				Image image = new Image(styledText.getDisplay(), imageData);
				Rectangle imageSize = image.getBounds();
	
				Label hoverLabel;
				// Reuse old hover labels if possible
				if (SourcePainterRegistry.hasTrashedHover()) {
					hoverLabel = SourcePainterRegistry.getNextTrashedHover();
					hoverLabel.setParent(styledText);
					hoverLabel.setVisible(true);
					System.out.println("recycled");
				} else {
					hoverLabel = new Label(styledText, 0);
					System.out.println("new drawn");
				}
				
				// Set hover label properties
				final Rectangle swtLabelBounds = menuLabel.getBounds();
				hoverLabel.setBounds(swtLabelBounds.x + 10, swtLabelBounds.y + charHeight, 
						imageSize.width + 10, imageSize.height + 10);
				hoverLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
				hoverLabel.setImage(image);
	
				// Register hover label to reduce future redraw
				SourcePainterRegistry.registerHover(id, hoverLabel);		
			}
		}
	
		@Override
		// Add visual cue (star sign) for label being right clickable
		public void mouseExit(MouseEvent e) {
			menuLabel.setText(kNotRightClickable);
			if (SourcePainterRegistry.hasHover(id))
				SourcePainterRegistry.getHover(id).setVisible(false);
		}
	
		@Override
		public void mouseEnter(MouseEvent e) {
			menuLabel.setText(kRightClickable);
	
		}
	}


	ArrowLabelPaintListener(PaintableEditor paintableEditor, StyledText styledText) {
		this.paintableEditor = paintableEditor;
		this.styledText = styledText;
		
		fileOSPath = paintableEditor.fileOSPath;
	}

	
	@Override
	// The main method to draw arrows, text labels and transition menus
	public void paintControl(PaintEvent e) {
		// Get file path in editor
		final int charHeight = styledText.getLineHeight();
		final int charWidth = e.gc.textExtent("a").x;
		
		drawTextLabels(e);
		drawArrows(e, charHeight, charWidth);
		drawTransitionMenus(charHeight, charWidth);
	}

	
	private void drawArrows(PaintEvent e, int charHeight, int charWidth) {
			List<SPArrow> arrows = SourcePainterRegistry.getArrowsOfFile(fileOSPath);
			
			// Setup environment
			e.gc.setLineWidth(2);
			e.gc.setAlpha(150);
			e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			e.gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
	
			if (arrows != null) {
				for (SPArrow arrow : arrows) {
	
					// Draw arrow
					int line1 = arrow.startL;
					int column1 = arrow.startC;
					int line2 = arrow.endL;
					int column2 = arrow.endC;
	
					if (line1 == line2 && column1 == column2) {
						System.err.println("Cannot draw arrow to self");
						continue;
					}
	
					// Decide the direction in which the arrow goes 
					int isDownwards = (line2 - line1 >= 0) ? 1 : -1;
					int isRightwards = (column2 - column1 >= 0) ? 1 : -1;
	
					// Get start and end points of the arrow
					int startOffset = getOffset(line1, column1);
					int endOffset = getOffset(line2, column2);
	
					// Remove invalid arrows
					if (startOffset < 0 || endOffset < 0) {
						System.err.println("Arrow " + arrow.toString() + " is invalid");
						SourcePainterRegistry.trashArrow(arrow);
						continue;
					}
	
					// Calculate startPoint, bezierPoint and endPoint
					Point startPoint = styledText.getLocationAtOffset(startOffset);
					Point endPoint = styledText.getLocationAtOffset(endOffset);
	
					
					Point bezierPoint;
					Point arcCenter;
	
					if (column1 == column2) {
						// Vertical arrow
						startPoint = new Point(startPoint.x, startPoint.y + charHeight / 2);
						endPoint = new Point(endPoint.x, endPoint.y + charHeight / 2);
						arcCenter = new Point((startPoint.x + endPoint.x) / 2, (startPoint.y + endPoint.y) / 2); 
						bezierPoint = new Point(arcCenter.x - 50, arcCenter.y);
	
					} else if (line1 == line2 || 
							Math.abs((double)(endPoint.y - startPoint.y) / (double)(endPoint.x - startPoint.x)) < 0.3) {
						// Horizontal arrow or near horizontal arrow
						startPoint = new Point(startPoint.x + charWidth / 2, startPoint.y + charHeight / 2);
						endPoint = new Point(endPoint.x + charWidth, endPoint.y + charHeight / 2);
						arcCenter = new Point((startPoint.x + endPoint.x) / 2, (startPoint.y + endPoint.y) / 2); 
	
						int yDelta = (int) (Math.abs(endPoint.x - startPoint.x) * 0.2);
						yDelta = Math.min(15, yDelta);
						yDelta = Math.max(50, yDelta);
						bezierPoint = new Point(arcCenter.x, arcCenter.y - yDelta); // 20
	
					}
					else {
						// New pos1.y: if is downwards adds charHeight, else doesn't change
						startPoint = new Point(startPoint.x + charWidth / 2, 
								startPoint.y + (charHeight - 3) * (isDownwards + 1) / 2 + 1 * (-isDownwards + 1) / 2);
						// New pos2.x: if is leftwards adds charWidth, else doesn't change
						endPoint = new Point(endPoint.x + charWidth * (-isRightwards + 1) / 2, endPoint.y + charHeight / 2);
	
	
						arcCenter = new Point((startPoint.x + endPoint.x) / 2, (startPoint.y + endPoint.y) / 2); 
						float lengthB = Math.abs(endPoint.x - arcCenter.x);
	
						double lengthC = Math.sqrt(Math.pow(arcCenter.x - startPoint.x, 2) + Math.pow(arcCenter.y - startPoint.y, 2));
						int startAngle = (int) (Math.round(Math.toDegrees(Math.acos(lengthB / lengthC)))) + 90; // in degrees
	
						// Triangle used to calculate the middle point of the curve
						double lengthCCurve = lengthC * 0.618;
						double lengthBCurve = Math.cos(Math.toRadians(startAngle - 90)) * lengthCCurve; 
						double lengthACurve = Math.sin(Math.toRadians(startAngle - 90)) * lengthCCurve;
	
						bezierPoint = new Point((int)(arcCenter.x - isRightwards * lengthBCurve), 
								(int)(arcCenter.y + isDownwards * lengthACurve));
					}
	
					// Prepare arrow head triangle
					float lengthHeadAlphaA = endPoint.x - bezierPoint.x;
					float lengthHeadAlphaB = endPoint.y - bezierPoint.y;
					double headAngleAlpha = Math.toDegrees(Math.atan(lengthHeadAlphaA / lengthHeadAlphaB)); // in degrees
	
					// beta = 90.0 - (30.0 - headAngleAlpha)
					double headAngleBeta = 60.0 + headAngleAlpha;
					double lengthHeadBetaA = Math.sin(Math.toRadians(headAngleBeta)) * 10;
					double lengthHeadBetaB = Math.cos(Math.toRadians(headAngleBeta)) * 10;
	
					// X: y1 < y2 -> + , y1 > y2 -> - 
					double triUpperRightX = endPoint.x + isDownwards * lengthHeadBetaB;
					double triUpperRightY = endPoint.y - isDownwards * lengthHeadBetaA;
	
					// gamma = 90.0 - headAngleAlpha - 30.0
					double headAngleGamma = 60.0 - headAngleAlpha;
					double lengthHeadGammaA = Math.sin(Math.toRadians(headAngleGamma)) * 10;
					double lengthHeadGammaB = Math.cos(Math.toRadians(headAngleGamma)) * 10;
	
					double triUpperLeftX = endPoint.x - isDownwards * lengthHeadGammaB;
					double triUpperLeftY = endPoint.y - isDownwards * lengthHeadGammaA;
	
	
					// Draw the arrow
					// Draw arrow shaft
					Path p = new Path(styledText.getDisplay());
					p.moveTo(startPoint.x, startPoint.y);
					p.quadTo(bezierPoint.x, bezierPoint.y, endPoint.x, endPoint.y);
	
					e.gc.drawPath(p);
	
					p.dispose();
	
					// Draw arrow head
					p = new Path(styledText.getDisplay());
					p.moveTo(endPoint.x, endPoint.y);
					p.lineTo((float)triUpperRightX, (float)triUpperRightY);
					p.lineTo((float)triUpperLeftX, (float)triUpperLeftY);
					p.close();
					e.gc.fillPath(p);
					p.dispose();
				}
				SourcePainterRegistry.emptyTrashOfArrows();
			}
		}


	/**
	 * Draw text labels of transition infos
	 * 
	 * @param e
	 */
	private void drawTextLabels(PaintEvent e) {
		// Set drawing enviroment
		Display currentDisplay = Display.getCurrent();
		Font infoFont = new Font(currentDisplay, "Lucida Grande", 11, 0);
	
		e.gc.setFont(infoFont);
		e.gc.setBackground(currentDisplay.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		e.gc.setForeground(currentDisplay.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
	
		List<SPTextLabel> labels = SourcePainterRegistry.getTextLabelsOfFile(fileOSPath);
		if (labels != null) {
			int editorWidth = styledText.getBounds().width;
			int scrollbarWidth = styledText.getVerticalBar().getSize().x;
			
			for (SPTextLabel label : labels) {
	
				// Remove invalid labels
				if (label.line >= styledText.getLineCount()) {
					System.err.println("Label on line " + label.line + " invalid. Removed.");
					SourcePainterRegistry.trashTextLabel(label);
				}
	
				// Calculate drawing positions
				int verticalPx = styledText.getLinePixel(label.line);
				int labelWidth = e.gc.textExtent(label.text).x;
	
				e.gc.drawText(label.text, editorWidth - labelWidth - scrollbarWidth, verticalPx);
	
			}
			SourcePainterRegistry.emptyTrashOfTextLabels();
		}
	}


	/**
	 * Draw context menus on labels with hover image
	 * 
	 * @param charHeight
	 * @param charWidth
	 */
	private void drawTransitionMenus(final int charHeight, final int charWidth) {
		List<SPContextMenu> menus = SourcePainterRegistry.getContextMenusOfFile(fileOSPath);

		if (menus != null) {
			for (final SPContextMenu menu : menus) {
				
				final int id = menu.shadowId;
				if (!SourcePainterRegistry.hasContextMenuLabel(id)) {
					
					// Get offset in code given line and column
					int offset = getOffset(menu.line, menu.column);
					if (offset < 0) 
						throw new InvalidOffsetException();
					
					// Create menu label
					final Label menuLabel = new Label(styledText, 0);
					
					Point offsetPoint = styledText.getLocationAtOffset(offset);
					// labelWidth = state length + padding
					int labelWidth = (menu.endColumn - menu.column + 1) * charWidth + 20;
					menuLabel.setBounds(offsetPoint.x - 10, offsetPoint.y, labelWidth, charHeight);

					// For debugging. Enable to see all labels in blue background which
					// is easier to understand what's going wrong.
//					swtLabel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

					// Add mouse listeners for showing/hiding visual cues and creating hover labels
					menuLabel.addMouseTrackListener(new SPMouseTrackListener(menuLabel, charHeight, id));
					
					// Add popup menu to the label and show it only if 
					// it is requested for better performance
					menuLabel.addListener(SWT.MenuDetect, new Listener() {

						@Override
						public void handleEvent(Event event) {
							if (menuLabel.getMenu() == null) {
								Menu popUp = createMenu(menu);
								menuLabel.setMenu(popUp);
							}
						}
					});

					// Redirect mouse click from label to code
					menuLabel.addListener(SWT.MouseDown, new Listener() {

						@Override
						public void handleEvent(Event event) {
							if (event.button == 1) {
								Rectangle labelBounds = menuLabel.getBounds();
								Point selection = new Point(event.x + labelBounds.x, event.y + labelBounds.y);
								int offset = styledText.getOffsetAtLocation(selection);

								styledText.setCaretOffset(offset);
							} 
						}
					});

					
					// Register menu labels to organize them later (like dispose of)
					SourcePainterRegistry.registerContextMenuLabel(menu.shadowId, menuLabel);
				}
			}
		}
	}

	
	/**
	 * Returns the offset in code given line and column. Notice that 
	 * line and column are 0-based.
	 * 
	 * @param line line
	 * @param column column
	 */
	private int getOffset(int line, int column)
	{
		// Error: not enough lines
		if (line >= styledText.getLineCount()) return -1;

		int offset = styledText.getOffsetAtLine(line);

		// Filter out leading tabs and spaces
		// char currentChar = styledText.getText(offset, offset).charAt(0);

		//	while (currentChar == 9 || currentChar == 32) {
		//		offset++;
		//		currentChar = styledText.getText(offset, offset).charAt(0);
		//	}
		
		offset += column;

		// Error: not enough columns
		if (line == styledText.getLineCount() - 1) {
			if (offset >= styledText.getCharCount())
				offset = -1; 
		} else if (offset >= styledText.getOffsetAtLine(line + 1)) 
			offset = -1;

		return offset;
	}

	
	/**
	 * Creates and returns a context menu given SPContextMenu
	 * 
	 * @param menu
	 * @return the created context menu
	 */
	private Menu createMenu(final SPContextMenu menu) {
		Menu popUp = new Menu(styledText.getShell(), SWT.POP_UP);

		for (final String fileName : menu.transitions.keySet()) {
			final File file = new File(fileName);

			// Add file title item
			MenuItem fileItem = new MenuItem(popUp, SWT.CASCADE);
			// TODO place transitions within this file on top
			if (fileName.equals(fileOSPath))
				fileItem.setText("Within this file");
			else {
				fileItem.setText("Within " + file.getName());
			}

			Menu submenu = new Menu(styledText.getShell(), SWT.DROP_DOWN);
			fileItem.setMenu(submenu);

			// Add transition items
			SortedMap<String, Integer> transitions = menu.transitions.get(fileName);
			for (String transitionName : transitions.keySet()) {
				final int lineNumber = transitions.get(transitionName);

				MenuItem transitionItem = new MenuItem(submenu, SWT.PUSH);
				transitionItem.setText(transitionName);

				// Add selection listener to each transition item
				transitionItem.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						// Hide the hover image
						if (SourcePainterRegistry.hasHover(menu.shadowId))
							SourcePainterRegistry.getHover(menu.shadowId).setVisible(false);

						// Set jump target
						if (fileName.equals(fileOSPath)) {
							paintableEditor.jumpToLine(lineNumber);
						} else {
							// Jump to methods in other files
							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

							for (IEditorReference editor : page.getEditorReferences()) {

								// TODO Restore Editor true/false ?
								if (editor.getTitle().equals(file.getName())) {
									page.bringToTop(editor.getPart(false));
									PaintableEditor pEditor = (PaintableEditor) page.getActiveEditor();
									pEditor.jumpToLine(lineNumber);
									break;
								}
							}		
						}
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
			}
		}
		return popUp;
	}
}