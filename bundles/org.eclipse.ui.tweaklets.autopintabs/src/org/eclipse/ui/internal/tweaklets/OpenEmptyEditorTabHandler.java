package org.eclipse.ui.internal.tweaklets;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchPage;

public class OpenEmptyEditorTabHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);
		WorkbenchPage page = (WorkbenchPage) window.getActivePage();
		if (page == null) {
			return null;
		}
		page.openEmptyTab();
		return null;
	}

}
