package com.yp.core.data.plugin.views;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IManagedConnection;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.actions.BuildActionGroup;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE;

import com.yp.core.data.plugin.model.Column;
import com.yp.core.data.plugin.model.Renderer;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Connect implements Initializable {

	@Inject
	Logger logger;
	private IWorkbenchSite site;

	public static final String PACKAGE_ID = "org.eclipse.jdt.ui.PackageExplorer";
	private static final String USERNAME = "org.eclipse.datatools.connectivity.db.username";
	private static final String USERPASSW = "org.eclipse.datatools.connectivity.db.password";
	private static final String SAVEPASSW = "org.eclipse.datatools.connectivity.db.savePWD";
	private static final String CONNECTION = "java.sql.Connection";

	@FXML
	private ComboBox<String> cmbConnProfile;
	@FXML
	private ComboBox<String> cmbSchemas;
	@FXML
	private ComboBox<String> cmbTables;
	@FXML
	private TextArea txtQuery;
	@FXML
	private TextField txtUserName;
	@FXML
	private TextField txtUserPassw;
	@FXML
	private Label txtPackageName;

	private Connection myConnection;
	private Renderer renderer;
	private Map<String, Column> keys;
	private Map<String, Column> columns;
	private List<Column> columnList;
	private IPackageFragment packagee;
	private BuildActionGroup actionGruop;
	private IWorkbenchWindow workBenchWindow;

	protected void setFxFocus() {
		cmbConnProfile.requestFocus();
	}

	@Override
	public void initialize(URL pLocation, ResourceBundle pResources) {
		fillProfiles();
		renderer = new Renderer(false);
		keys = new HashMap<>();
		columns = new HashMap<>();
		columnList = new ArrayList<>();
	}

	private IConnectionProfile[] profiles;
	private List<String> profileList;
	private IConnectionProfile sellectedProfile;

	private void fillProfiles() {
		ProfileManager pm = ProfileManager.getInstance();
		if (pm != null) {
			profiles = pm.getProfiles();
			profileList = new ArrayList<>(profiles.length);
			for (IConnectionProfile p : profiles) {
				profileList.add(p.getName());
			}
			cmbConnProfile.setItems(FXCollections.observableArrayList(profileList));
		}

	}

	private void buildConnection() {
		if (getSellectedProfile() != null) {
			Properties p = sellectedProfile.getBaseProperties();
			if (!Column.isNull(txtUserName.getText())) {
				p.put(USERNAME, txtUserName.getText());
				p.put(USERPASSW, txtUserPassw.getText());
				p.put(SAVEPASSW, "true");
			}
			sellectedProfile.setBaseProperties(p);
			sellectedProfile.connect();
			IManagedConnection mc = sellectedProfile.getManagedConnection(CONNECTION);
			if (mc != null)
				myConnection = (Connection) mc.getConnection().getRawConnection();
		}
	}

	private List<String> schemaList;

	private void fillSchemaNames(DatabaseMetaData meta) {
		try (ResultSet res = meta.getSchemas()) {
			schemaList = new ArrayList<>();
			while (res.next()) {
				String catalog = res.getString("TABLE_CATALOG");
				String schema = res.getString("TABLE_SCHEM");
				if (!Column.isNull(catalog))
					schema = catalog + ":" + schema;
				schemaList.add(schema);

			}
			cmbSchemas.setItems(FXCollections.observableArrayList(schemaList));
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	private void fillSchemaNames() {
		try {
			if (myConnection != null) {
				fillSchemaNames(myConnection.getMetaData());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

	}

	private List<String> tableList;

	private void fillTableNames(String pSchemaName, DatabaseMetaData meta) {
		String catalog = null;
		String schema = pSchemaName;
		String[] temp = pSchemaName.split(":");
		if (temp.length > 1) {
			catalog = temp[0];
			schema = temp[1];
		}
		String[] types = { "TABLE" };
		try (ResultSet res = meta.getTables(catalog, schema, null, types)) {
			tableList = new ArrayList<>();
			while (res.next()) {
				tableList.add(res.getString("TABLE_NAME"));
			}
			cmbTables.setItems(FXCollections.observableArrayList(tableList));
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	private void fillTableNames(String pSchemaName) {
		if (myConnection != null) {
			try {
				fillTableNames(pSchemaName, myConnection.getMetaData());
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage());
			}
		}

	}

	@FXML
	public void onConnProfileChanged(ActionEvent event) {
		try {
			if (myConnection != null && !myConnection.isClosed())
				myConnection.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		if (sellectedProfile != null) {
			sellectedProfile.disconnect();
			sellectedProfile = null;
		}
		getSellectedProfile();
	}

	private IConnectionProfile getSellectedProfile() {
		if (sellectedProfile == null) {
			int i = cmbConnProfile.getSelectionModel().getSelectedIndex();
			if (i > -1) {
				sellectedProfile = profiles[cmbConnProfile.getSelectionModel().getSelectedIndex()];
				Properties p = sellectedProfile.getBaseProperties();
				txtUserName.setText(p.getProperty(USERNAME));
				txtUserPassw.setText(p.getProperty(USERPASSW));
			}
		}
		return sellectedProfile;
	}

	@FXML
	public void onSchemasChanged(ActionEvent event) {
		fillTableNames(cmbSchemas.getSelectionModel().getSelectedItem());
	}

	@FXML
	public void onTablesChanged(ActionEvent event) {
		renderer.setTableName(cmbTables.getSelectionModel().getSelectedItem());
	}

	@FXML
	public void onConnectDbClicked(final ActionEvent arg0) {
		buildConnection();
		fillSchemaNames();
	}

	@FXML
	public void onGenerateDataEntityClicked(final ActionEvent arg0) {
		buildConnection();
		prepareRenderer();
		if (renderer.isReady()) {
			String file = renderer.render(packagee);
			actionGruop.getRefreshAction().run();

			File f = new File(file);
			IPath ipath = new Path(f.getAbsolutePath());
			IWorkbenchPage page = workBenchWindow.getActivePage();
			try {
				IDE.openEditorOnFileStore(page, EFS.getLocalFileSystem().getStore(ipath));
			} catch (PartInitException e) {
				logger.log(Level.SEVERE, e.getMessage());
			}
			renderer = new Renderer(false);
		}

		try {
			if (myConnection != null && !myConnection.isClosed())
				myConnection.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		if (sellectedProfile != null) {
			sellectedProfile.disconnect();
			sellectedProfile = null;
		}
	}

	private void generatColumnList() {
		String query = txtQuery.getText();
		if (Column.isNull(query) || query.length() < 10)
			query = "select * from " + renderer.getTableFullName();

		try (java.sql.PreparedStatement ps = myConnection.prepareStatement(query);) {
			ResultSetMetaData resMeta = ps.getMetaData();
			generateColumnList(resMeta);
			renderer.setColumnList(columnList);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	private void prepareRenderer() {
		try {
			if (myConnection != null) {
				renderer.setSchemaName(cmbSchemas.getSelectionModel().getSelectedItem());
				renderer.setTableName(cmbTables.getSelectionModel().getSelectedItem());
				DatabaseMetaData connMeta = myConnection.getMetaData();
				renderer.setDriverName(connMeta.getDriverName());
				generateKeys(connMeta);
				generateColumnNames(connMeta);
				generatColumnList();
				renderer.setReady(true);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage());
		} finally {
			try {
				if (myConnection != null && !myConnection.isClosed())
					myConnection.close();
			} catch (SQLException e) {
				logger.log(Level.SEVERE, e.getMessage());
			}
		}
	}

	private void generateKeys(DatabaseMetaData pConnMeta) {
		keys.clear();
		String arg1, arg2;
		if (renderer.isMicrosoftSql()) {
			arg1 = renderer.getSchemaName();
			arg2 = "dbo";
		} else {
			arg1 = null;
			arg2 = renderer.getSchemaName();
		}
		try (ResultSet rs = pConnMeta.getPrimaryKeys(arg1, arg2, renderer.getTableName())) {
			Column col;
			while (rs.next()) {
				col = new Column(rs.getString(4), "", false);
				keys.put(col.getColumnName(), col);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

	}

	private void generateColumnNames(DatabaseMetaData pConnMeta) {
		columns.clear();
		String arg1, arg2;
		if (renderer.isMicrosoftSql()) {
			arg1 = renderer.getSchemaName();
			arg2 = "dbo";
		} else {
			arg1 = null;
			arg2 = renderer.getSchemaName();
		}
		try (ResultSet rs = pConnMeta.getColumns(arg1, arg2, renderer.getTableName(), null)) {
			Column col;
			while (rs.next()) {
				col = new Column(rs.getString(4), "", false);
				if (!Renderer.USER_COLUMNS.containsKey(col.getColumnName()))
					columns.put(col.getColumnName(), col);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

	}

	private void generateColumnList(ResultSetMetaData pResMeta) {
		columnList.clear();
		try {
			int columnCount = pResMeta.getColumnCount();
			String columnName;
			for (int i = 1; i <= columnCount; i++) {
				columnName = pResMeta.getColumnName(i);
				Column newColumn = new Column(columnName, pResMeta.getColumnClassName(i), pResMeta.isAutoIncrement(i));
				if (!Renderer.USER_COLUMNS.containsKey(newColumn.getColumnName())) {
					if (keys.containsKey(newColumn.getColumnName())) {
						newColumn.setKey(true);
						if (newColumn.isAutoIncrement())
							keys.get(newColumn.getColumnName()).setAutoIncrement(true);
					}
					if (!columns.containsKey(newColumn.getColumnName()))
						newColumn.setReadonly(true);
					columnList.add(newColumn);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	protected void printMedaData(DatabaseMetaData pM) throws SQLException {
		System.out.println("pM.getCatalogSeparator()" + pM.getCatalogSeparator());
		System.out.println("pM.getDriverName()" + pM.getDriverName());
		System.out.println("pM.getDriverVersion()" + pM.getDriverVersion());
		System.out.println("pM.getSchemaTerm()" + pM.getSchemaTerm());

		try (ResultSet rs = pM.getSchemas();) {
			while (rs.next()) {
				System.out.println("pM.getSchemaName()" + rs.getString(1));
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

		try (ResultSet rs = pM.getCatalogs();) {
			while (rs.next()) {
				System.out.println("pM.getCatalogs()" + rs.getString(1));
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	public IPackageFragment getPackagee() {
		return packagee;
	}

	public void setSite(IWorkbenchSite pSite) {
		site = pSite;
		workBenchWindow = site.getWorkbenchWindow();
		actionGruop = new BuildActionGroup(site, null);
		packagee = (IPackageFragment) site.getShell().getData("package");

		if (packagee != null)
			txtPackageName.setText(packagee.getElementName());
	}

	@FXML
	public void onBrowseClicked(final ActionEvent arg0) {
		Shell shell = this.site.getShell();

		ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());

		dialog.setTitle("My Dialog Title");
		dialog.setMessage("My Dialog Message");
		try {
			Set<String> pack = new HashSet<>();
			listOfPackage2(packagee.getJavaProject().getChildren(), pack);
			dialog.setElements(pack.toArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (dialog.open() == ElementListSelectionDialog.OK) {
			Object[] types = dialog.getResult();
		}

	}

//	 public class q {
//	        public static void main(String args[]) {
//	            Package[] pack = Package.getPackages();
//
//	          // print all packages, one by one
//	            for (int i = 0; i < pack.length; i++) {
//	                String a = pack[i].toString()  ;
//	                System.out.println(a.replaceAll("package ", ""));
//	            }
//	        }
//	    }
	 
	public static void listOfPackage2(IJavaElement[] list, Set<String> pack) {		
		for (IJavaElement je : list) {
			System.out.println(je.getElementName());
			if (je.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				pack.add(je.getElementName());
			}
		}
		
	}

	public static void listOfPackage(File directory, Set<String> pack) {
		// File directory = new File(directoryName);

		// get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				String path = file.getPath();
				String packName = path.substring(path.indexOf("src") + 4, path.lastIndexOf('\\'));
				pack.add(packName.replace('\\', '.'));
			} else if (file.isDirectory()) {
				listOfPackage(file, pack);
				// listOfPackage(file.getAbsolutePath(), pack);
			}
		}
	}
}