package com.yp.core.data.plugin.model;

import static java.util.stream.Collectors.toMap;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.core.IPackageFragment;

public class Renderer {

//	@Inject
//	Logger logger;

	private String connProfile, schemaName, tableName, tableFullName, userName, userPassw, driverName;
	private String schemaSeperator = ".";

	private boolean isReady, isMicrosoftSql;
	private List<Column> columnList, keyList;
	private List<String> importList;
	public static final Map<String, String> USER_COLUMNS = Stream
			.of(new SimpleEntry<>("owner", "owner"), new SimpleEntry<>("remaddress", "remaddress"),
					new SimpleEntry<>("datetime", "datetime"), new SimpleEntry<>("last_owner", "last_owner"),
					new SimpleEntry<>("last_remaddress", "last_remaddress"),
					new SimpleEntry<>("last_datetime", "last_datetime"))
			.collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));

	public Renderer(boolean pSuccess) {
		super();
		isReady = pSuccess;
		columnList = new ArrayList<>();
		importList = new ArrayList<>();
		keyList = new ArrayList<>();
		isMicrosoftSql = false;
	}

	public Renderer(List<Column> pColumnList) {
		super();
		isMicrosoftSql = false;
		setColumnList(pColumnList);
	}

	public String getConnProfile() {
		return connProfile;
	}

	public void setConnProfile(String pConnProfile) {
		connProfile = pConnProfile;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String pSchemaName) {
		schemaName = pSchemaName;
		// schemaName = pSchemaName.toLowerCase(Column.LOCALE_EN);
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String pTableName) {
		tableName = pTableName;
		// tableName = pTableName.toLowerCase(Column.LOCALE_EN);
	}

	public String getTableFullName() {
		if (tableFullName == null) {
			tableFullName = schemaName + schemaSeperator + tableName;
		}
		return tableFullName;
	}

	public void setTableFullName(String pTableFullName) {
		tableFullName = pTableFullName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String pUserName) {
		if (!Column.isNull(pUserName))
			userName = pUserName.trim();
	}

	public String getUserPassw() {
		return userPassw;
	}

	public void setUserPassw(String pUserPassw) {
		userPassw = pUserPassw;
	}

	public String getDriverName() {
		return driverName;
	}

	public void setDriverName(String pDriverName) {
		driverName = pDriverName;
		if (!Column.isNull(pDriverName) && pDriverName.startsWith("Microsoft")) {
			schemaSeperator = ".dbo.";
			isMicrosoftSql = true;
		}
	}

	public String getSchemaSeperator() {
		return schemaSeperator;
	}

	public void setSchemaSeperator(String pSchemaSeperator) {
		schemaSeperator = pSchemaSeperator;
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean pReady) {
		isReady = pReady;
	}

	public boolean isMicrosoftSql() {
		return isMicrosoftSql;
	}

	public void setMicrosoftSql(boolean pIsMicrosoftSql) {
		isMicrosoftSql = pIsMicrosoftSql;
	}

	public List<Column> getColumnList() {
		return columnList;
	}

	public void setColumnList(List<Column> pColumnList) {
		columnList = pColumnList;
		HashMap<String, String> imports = new HashMap<>();
		keyList.clear();
		imports.put("com.yp.core.entity.DataEntity", "com.yp.core.entity.DataEntity");
		for (Column column : pColumnList) {
			imports.put(column.getColumnTypeFullName(), column.getColumnTypeFullName());
			if (column.isDate()) {
				imports.put("com.yp.core.tools.DateTime", "com.yp.core.tools.DateTime");
				imports.put("java.util.Date", "java.util.Date");
			}
			if (column.isKey())
				keyList.add(column);

		}

		importList.clear();
		imports.forEach((k, v) -> importList.add(k));
	}

	public List<Column> getKeyList() {
		return keyList;
	}

	public List<String> getImportList() {
		return importList;
	}

	private static final String EOL_WITH_AFTER_COMMA = ";\n";
	private static final String EOL_DOUBLE = "\n\n";
	private static final String SET = "		set(%s, p%s)";

	private void render(String file, String pPackage, String className) {
		try (FileWriter output = new FileWriter(file); BufferedWriter writer = new BufferedWriter(output);) {
			writer.write("package " + pPackage);
			writer.write(EOL_WITH_AFTER_COMMA);
			writer.write(EOL_DOUBLE);
			for (String impt : importList) {
				writer.write("import " + impt);
				writer.write(EOL_WITH_AFTER_COMMA);
			}
			writer.write(EOL_DOUBLE);
			writer.write(String.format("public class %s extends DataEntity {", className));

			writer.write(EOL_DOUBLE);
			writeConstructors(writer, className);
			writer.write(EOL_DOUBLE);

			for (Column column : columnList) {
				writeColumn(writer, column);
			}

			writer.write("	@Override\n");
			writer.write("	public String getSchemaName() {\n");
			writer.write("		return SCHEMA_NAME");
			writer.write(EOL_WITH_AFTER_COMMA);
			writer.write("	}");
			writer.write(EOL_DOUBLE);

			writer.write("	@Override\n");
			writer.write("	public String getTableName() {\n");
			writer.write("		return TABLE_NAME");
			writer.write(EOL_WITH_AFTER_COMMA);
			writer.write("	}");
			writer.write(EOL_DOUBLE);
			writer.write("}");
			writer.flush();
		} catch (Exception e) {
			// logger.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		}
	}

	public String render(IPackageFragment pu) {
		String file = null;
		try {
			String path = pu.getCorrespondingResource().getLocation().toString();
			String className = Column.ucaseFirsChar(getTableName());
			file = path + "/" + className + ".java";
			render(file, pu.getElementName(), className);
		} catch (Exception e) {
			// logger.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		}
		return file;
	}

	private void writeConstructors(BufferedWriter pWriter, String className) throws IOException {
		pWriter.write("	private static String SCHEMA_NAME = \"" + getSchemaName() + "\"");
		pWriter.write(EOL_WITH_AFTER_COMMA);
		pWriter.write("	private static String TABLE_NAME = \"" + getTableName() + "\"");
		pWriter.write(EOL_WITH_AFTER_COMMA);
		pWriter.write(EOL_DOUBLE);

		pWriter.write("	public " + className + "(){\n");
		pWriter.write("		super()");
		pWriter.write(EOL_WITH_AFTER_COMMA);
		pWriter.write("		className = \"" + className + "\"");
		pWriter.write(EOL_WITH_AFTER_COMMA);

		if (!keyList.isEmpty()) {
			String comma = "";
			pWriter.write("		setPrimaryKeys(");
			for (Column column : keyList) {
				pWriter.write(comma + column.getFieldName());
				comma = ", ";
			}
			pWriter.write(")");
			pWriter.write(EOL_WITH_AFTER_COMMA);
			for (Column column : keyList) {
				if (column.isReadonly()) {
					pWriter.write("		setFieldReadonly(" + column.getFieldName() + ", true)");
					pWriter.write(EOL_WITH_AFTER_COMMA);
				}
			}
		}
		pWriter.write("	}");

		pWriter.write(EOL_DOUBLE);

		if (!keyList.isEmpty()) {
			String comma = "";
			pWriter.write("	public " + className + "(");
			for (Column column : keyList) {
				pWriter.write(comma + column.getColumnType() + " p" + column.getColumnName());
				comma = ", ";
			}
			pWriter.write("){\n");
			pWriter.write("		this()");
			pWriter.write(EOL_WITH_AFTER_COMMA);
			for (Column column : keyList) {
				pWriter.write(String.format(SET, column.getFieldName(), column.getColumnName()));
				pWriter.write(EOL_WITH_AFTER_COMMA);
			}

			pWriter.write("	}");
		}
	}

	private void writeColumn(BufferedWriter pWriter, Column pColumn) throws IOException {
		int i = 0;
		if (!pColumn.isReadonly()) {
			if (pColumn.isDate())
				i = 2;
		} else {
			i = 1;
		}
		String field = functions[i].replace("@FieldName", pColumn.getFieldName())
				.replace("@FunctionName", pColumn.getFunctionName()).replace("@Type", pColumn.getColumnType())
				.replace("@ColumnName", pColumn.getColumnName());
		pWriter.write(field);
		pWriter.write(EOL_DOUBLE);
	}

	private static String[] functions = new String[] { getFileResource("function1.txt"), // i=0 writable
			getFileResource("function2.txt"), // i=1 readonly
			getFileResource("function3.txt"), // i=2 date
			getFileResource("function4.txt") }; // i= 3 datetime

	private static String getFileResource(String pFileName) {
		try (BufferedInputStream bis = new BufferedInputStream(
				Renderer.class.getClassLoader().getResourceAsStream("META-INF/" + pFileName));
				ByteArrayOutputStream buf = new ByteArrayOutputStream();) {
			int result = bis.read();
			while (result != -1) {
				byte b = (byte) result;
				buf.write(b);
				result = bis.read();
			}
			return buf.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

}
