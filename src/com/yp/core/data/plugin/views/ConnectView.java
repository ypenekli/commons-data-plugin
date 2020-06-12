package com.yp.core.data.plugin.views;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.eclipse.fx.ui.workbench3.FXViewPart;
import org.eclipse.ui.IWorkbenchPart;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class ConnectView extends FXViewPart {

	public static final String VIEW_ID = "com.yp.core.data.plugin.views.Connect";

	@Inject
	Logger logger;

	private Connect connect;

	@Override
	protected Scene createFxScene() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Connect.fxml"),
				ResourceBundle.getBundle("Messages"));
		Pane pane;
		try {
			pane = loader.load();
			connect = (Connect) loader.getController();
			IWorkbenchPart parentPart = (IWorkbenchPart) getSite().getShell().getData("part");
			if (parentPart != null) {
				connect.setSite(parentPart.getSite());
				return new Scene(pane);
			}

		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

		return new Scene(new BorderPane());
	}

	@Override
	protected void setFxFocus() {
		connect.setFxFocus();
	}

}
