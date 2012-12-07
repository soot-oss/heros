package heros.debugui.drawing;

import heros.debugsupport.SerializableEdgeData;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class EdgeDrawing {

	protected final IJavaProject javaProject;

	public EdgeDrawing(String projectName) {
		if(projectName!=null) {
			IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			javaProject = JavaCore.create(proj);
		} else {
			javaProject = null;
		}
	}

	public void openEditorAndDrawEdge(final SerializableEdgeData edge) {
		drawEdge(edge);
		openEditorAndJumpToLine(edge);
	}

	private void drawEdge(SerializableEdgeData edge) {
		IPath path;
		try {
			path = javaProject.findType(edge.className).getPath();
			IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			String osString = file.getLocation().toOSString();
			SourcePainterRegistry.registerArrow(osString, edge.startLine, edge.startColumn, edge.endLine, edge.endColumn);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void openEditorAndJumpToLine(final SerializableEdgeData edge) {
		Display.getDefault().asyncExec(new Runnable() {			
			@Override
			public void run() {
				try {
					String className = edge.className;
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IPath path = javaProject.findType(className).getPath();
					IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
					if(file!=null) {
						ITextEditor editor = (ITextEditor) IDE.openEditor(page, file);
						moveToLine(edge.startLine, editor);
					}
					
				} catch (JavaModelException e) {
					e.printStackTrace();
				} catch (PartInitException e) {
					e.printStackTrace();
				}
				
			}

			private void moveToLine(final int line, ITextEditor editor) {
				IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				if (document != null) {
					IRegion lineInfo = null;
					try {
						// line count internaly starts with 0, and not
						// with 1 like in
						// GUI
						lineInfo = document
								.getLineInformation(line-1);
					} catch (BadLocationException e) {
						// ignored because line number may not really
						// exist in document,
						// we guess this...
					}
					if (lineInfo != null) {
						editor.selectAndReveal(lineInfo.getOffset(),
								lineInfo.getLength());
					}
				}
			}
		});
	}
	

}
