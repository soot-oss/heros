package de.ishuo.sourcepainter.handler;

import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import de.ishuo.sourcepainter.PaintableEditor;
import de.ishuo.sourcepainter.SPArrow;
import de.ishuo.sourcepainter.SourcePainterRegistry;


public class RedrawContextMenuLabelsHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorInput input = page.getActiveEditor().getEditorInput();

		String fileOSPath = SourcePainterRegistry.getOSPath(input.getName());
		System.out.println(fileOSPath);
		
		SourcePainterRegistry.removeAllContextMenuLabelsOfFile(fileOSPath);
		SourcePainterRegistry.trashAllHoversOfFile(fileOSPath);
		
		
		return null;
	}

}
