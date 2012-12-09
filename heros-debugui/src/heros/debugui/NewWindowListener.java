package heros.debugui;

import heros.debugui.drawing.EditorPaintListener;
import heros.debugui.lifecycle.NewEditorListener;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

@SuppressWarnings("restriction")
public class NewWindowListener implements IWindowListener {
	/**
	 * Registers appropriate listeners that will cause debug information to be painted into
	 * a potential {@link {@link JavaEditor}} shown in the given window.
	 */
	public static void registerListenersWithWindow(IWorkbenchWindow window) {
		for(IWorkbenchPage page: window.getPages()) {
			//register listeners for all currently open Java editors
			for(IEditorReference editorRef: page.getEditorReferences()) {
				IEditorPart editor = editorRef.getEditor(false);
				if(editor instanceof JavaEditor) {
					JavaEditor javaEditor = (JavaEditor) editor;
					EditorPaintListener.register(javaEditor);
				}
			}
			//register listeners for all editors will be created in the future
			page.addPartListener(new NewEditorListener());
		}
	}
	

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		registerListenersWithWindow(window);
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
	}
}