package heros.debugui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class HerosDebugLaunchDelegate extends JavaLaunchDelegate {

	public HerosDebugLaunchDelegate() {
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		super.launch(configuration, "run", launch, monitor);
	}
	
	public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
		//todo replace arguments with call to analysis etc.
		return super.getProgramArguments(configuration);
	}
	
	@Override
	public String getMainTypeName(ILaunchConfiguration configuration)
			throws CoreException {
		// TODO Auto-generated method stub
		return super.getMainTypeName(configuration);
	}

}
