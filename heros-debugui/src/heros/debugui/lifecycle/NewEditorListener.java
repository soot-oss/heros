package heros.debugui.lifecycle;

import heros.debugui.drawing.EditorPaintListener;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * When a new editor is opened, associates an {@link EditorPaintListener} with it.
 */
@SuppressWarnings("restriction")
public class NewEditorListener implements IPartListener2 {
	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		if(partRef instanceof IEditorReference) {
			IEditorReference editorReference = (IEditorReference) partRef;
			IEditorPart editor = editorReference.getEditor(false);
			if(editor instanceof JavaEditor) {
				JavaEditor javaEditor = (JavaEditor) editor;
				EditorPaintListener.register(javaEditor);
			}
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
	}
}