package heros.debugui.launching;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class HerosDebugLaunchDelegate extends JavaLaunchDelegate {

	private IJavaProject analysisProject;
	private String analysisMainClass;

	public HerosDebugLaunchDelegate() {
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String analysisProjectName = configuration.getAttribute(HerosLaunchConstants.PROJ_NAME_ID, "");
		analysisMainClass = configuration.getAttribute(HerosLaunchConstants.MAIN_CLASS_ID, "");
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(analysisProjectName);
		analysisProject = JavaCore.create(project);
		
		super.launch(configuration, "run", launch, monitor);
	}
	
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		return super.getProgramArguments(configuration);
	}
	
	@Override
	public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return analysisMainClass;
	}
	
	
	public URL[] projectClassPath() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IClasspathEntry[] cp;
		try {
			cp = analysisProject.getResolvedClasspath(true);
			Set<URL> urls = new HashSet<URL>();
			String uriString = workspace.getRoot().getFile(
					analysisProject.getOutputLocation()).getLocationURI().toString()
					+ "/";
			urls.add(new URI(uriString).toURL());
			for (IClasspathEntry entry : cp) {
				File file = entry.getPath().toFile();
				URL url = file.toURI().toURL();
				urls.add(url);
			}
			URL[] array = new URL[urls.size()];
			urls.toArray(array);
			return array;
		} catch (JavaModelException e) {
			e.printStackTrace();
			return new URL[0];
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new URL[0];
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return new URL[0];
		}
	}
	
	public String projectClassPathAsString() {		
		StringBuffer cp = new StringBuffer();
		for (URL url : projectClassPath()) {
			cp.append(url.getPath());
			cp.append(File.pathSeparator);
		}
		
		return cp.toString();
	}

}
