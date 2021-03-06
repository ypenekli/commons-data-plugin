package com.yp.core.data.plugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.yp.core.data.plugin.views.ConnectView;

public class ConnectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IWorkbenchPart parentPart = window.getActivePage().getActivePart();
		try {
			window.getShell().setData("part", parentPart);
			IPackageFragment packagee = getPackage(event);
			if (packagee != null) {
				window.getShell().setData("package", packagee);
				IViewPart part = window.getActivePage().showView(ConnectView.VIEW_ID);
				window.getActivePage().hideView(part);
				window.getActivePage().showView(ConnectView.VIEW_ID);

			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

	private IPackageFragment getPackage(ExecutionEvent event) {
		ISelection select = HandlerUtil.getActiveMenuSelection(event);
		if (select != null) {
			IStructuredSelection sel = (IStructuredSelection) select;
			Object firstElement = sel.getFirstElement();
			if (firstElement instanceof IPackageFragment) {
				return ((IPackageFragment) firstElement);
			}
		}
		return null;
	}
}
