package heros.debugui;

import heros.debugsupport.SerializableEdgeData;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

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

	public void drawEdge(final SerializableEdgeData edge) {
		Display.getDefault().asyncExec(new Runnable() {			
			@Override
			public void run() {
				try {
					String className = edge.className;
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IPath path = javaProject.findType(className).getPath();
					IFile file = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
					if(file!=null)
						IDE.openEditor(page, file);
				} catch (JavaModelException e) {
					e.printStackTrace();
				} catch (PartInitException e) {
					e.printStackTrace();
				}
				
			}
		});
	}
	

}
