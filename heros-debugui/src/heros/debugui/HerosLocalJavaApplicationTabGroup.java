package heros.debugui;

import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LocalJavaApplicationTabGroup;

@SuppressWarnings("restriction")
public class HerosLocalJavaApplicationTabGroup extends LocalJavaApplicationTabGroup {
	
	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new AnalysisDriverTab(),
				new JavaMainTab(),
				new JavaJRETab(),
				new JavaClasspathTab(),
				new SourceLookupTab(),
				new CommonTab()
			};
			setTabs(tabs);
	}
	
	

}


