package org.eclipse.e4.ui.examples.legacy.workbench;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.services.context.IContextFunction;
import org.eclipse.e4.core.services.context.IEclipseContext;

public class WorkspaceValue implements IContextFunction {

	public Object compute(IEclipseContext context, Object[] arguments) {
		ResourcesPlugin.getPlugin().getPluginPreferences().setValue(ResourcesPlugin.PREF_AUTO_REFRESH, true);
		return ResourcesPlugin.getWorkspace();
	}

}