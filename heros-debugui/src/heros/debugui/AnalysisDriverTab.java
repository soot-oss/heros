package heros.debugui;

import static heros.debugui.launching.HerosLaunchConstants.MAIN_CLASS_ID;
import static heros.debugui.launching.HerosLaunchConstants.PROJ_NAME_ID;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * This is a tab for setting the project and main class of the analysis.
 * Uses a bit of a hack to adapt an existing JavaMainTab. We need to 
 * override some methods to take care of storing the values
 * under the right attributes.
 */
@SuppressWarnings("restriction")
public class AnalysisDriverTab extends JavaMainTab {
	
	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
		((GridLayout)comp.getLayout()).verticalSpacing = 0;
		createProjectEditor(comp);
		createVerticalSpacer(comp, 1);
		createMainTypeEditor(comp, LauncherMessages.JavaMainTab_Main_cla_ss__4);
		setControl(comp);
		
		Group group = (Group) comp.getChildren()[2];
		Control[] children = group.getChildren();
		children[2].setVisible(false);
		children[2].setEnabled(false);
		children[3].setVisible(false);
		children[3].setEnabled(false);
		children[4].setVisible(false);
		children[4].setEnabled(false);
	}
	
	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		updateMainTypeFromConfig(config);
		updateProjectFromConfig(config);
	}

	@Override
	protected void updateMainTypeFromConfig(ILaunchConfiguration config) {
		String mainTypeName = EMPTY_STRING;
		try {
			mainTypeName = config.getAttribute(MAIN_CLASS_ID, EMPTY_STRING);
		}
		catch (CoreException ce) {JDIDebugUIPlugin.log(ce);}	
		fMainText.setText(mainTypeName);	
	}
	
	@Override
	protected void initializeJavaProject(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		IJavaProject javaProject = javaElement.getJavaProject();
		String name = null;
		if (javaProject != null && javaProject.exists()) {
			name = javaProject.getElementName();
		}
		config.setAttribute(PROJ_NAME_ID, name);
	}	
	
	protected void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName = EMPTY_STRING;
		try {
			projectName = config.getAttribute(PROJ_NAME_ID, EMPTY_STRING);	
		}
		catch (CoreException ce) {
			setErrorMessage(ce.getStatus().getMessage());
		}
		fProjText.setText(projectName);
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement javaElement = getContext();
		if (javaElement != null) {
			initializeJavaProject(javaElement, config);
		}
		else {
			config.setAttribute(PROJ_NAME_ID, EMPTY_STRING);
		}
		initializeMainTypeAndName(javaElement, config);
	}
	
	protected void initializeMainTypeAndName(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		String name = null;
		if (javaElement instanceof IMember) {
			IMember member = (IMember)javaElement;
			if (member.isBinary()) {
				javaElement = member.getClassFile();
			}
			else {
				javaElement = member.getCompilationUnit();
			}
		}
		if (javaElement instanceof ICompilationUnit || javaElement instanceof IClassFile) {
			try {
				IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[]{javaElement}, false);
				MainMethodSearchEngine engine = new MainMethodSearchEngine();
				IType[] types = engine.searchMainMethods(getLaunchConfigurationDialog(), scope, false);				
				if (types != null && (types.length > 0)) {
					// Simply grab the first main type found in the searched element
					name = types[0].getFullyQualifiedName();
				}
			}
			catch (InterruptedException ie) {JDIDebugUIPlugin.log(ie);} 
			catch (InvocationTargetException ite) {JDIDebugUIPlugin.log(ite);}
		}
		if (name == null) {
			name = EMPTY_STRING;
		}
		config.setAttribute(MAIN_CLASS_ID, name);
		if (name.length() > 0) {
			int index = name.lastIndexOf('.');
			if (index > 0) {
				name = name.substring(index + 1);
			}	
			name = getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
		}
	}
	
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		String projName = fProjText.getText().trim();
		config.setAttribute(PROJ_NAME_ID, projName);
		
		String mainClassName = fMainText.getText().trim();
		config.setAttribute(MAIN_CLASS_ID, mainClassName);
	}	

	
}
