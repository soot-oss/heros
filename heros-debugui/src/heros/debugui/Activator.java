package heros.debugui;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("restriction")
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "heros-debugui";

	// The shared instance
	private static Activator plugin;
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		registerListeners();
	}

	/**
	 * Registers appropriate listeners that will cause debug information to be painted into
	 * {@link {@link JavaEditor}}s.
	 */
	private void registerListeners() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		//for each currently open window, register listeners with their java editors
		for (int i = 0; i < windows.length; i++) {
			if (windows[i] != null) {
				IWorkbenchWindow window = windows[i];
				NewWindowListener.registerListenersWithWindow(window);
			}
		}
		//for each window yet to be opened, do the same
		workbench.addWindowListener(new NewWindowListener());
	}

	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
