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

	private String connProfile, schemaName, tableName, tableFullName, userName, userPassw, driverName;
	private String schemaSeperator = ".";

	private boolean isReady, isMicrosoftSql;
	private List<Column> columnList, keyList;
	private List<String> importList;
	public static final Map<String, String> USER_COLUMNS = Stream
			.of(new SimpleEntry<>("client_name", "client_name"), new SimpleEntry<>("client_ip", "client_ip"),
					new SimpleEntry<>("client_datetime", "client_datetime"),
					new SimpleEntry<>("last_client_name", "last_client_name"),
					new SimpleEntry<>("last_client_ip", "last_client_ip"),
					new SimpleEntry<>("last_client_datetime", "last_client_datetime"))
			.collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));

	public Renderer(boolean pSuccess) {
		super();
		isReady = pSuccess;
		columnList = new ArrayList<>();
		importList = new ArrayList<>();
		keyList = new ArrayList<>();
		isMicrosoftSql = false;
	}

	public Renderer(String pSchemaName, String pTableName) {
		this(false);
		setSchemaName(pSchemaName);
		setTableName(pTableName);
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
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String pTableName) {
		tableName = pTableName;
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
			if (!column.getColumnTypeFullName().startsWith("java.lang."))
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
	private static final String CONSTRUCTOR_SET1 = "%s%s p%s";
	private static final String CONSTRUCTOR_SET2 = "\t\tset(%s, p%s)";

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

			writer.write("\t@Override\n");
			writer.write("\tpublic String getSchemaName() {\n");
			writer.write("\t\treturn schemaName");
			writer.write(EOL_WITH_AFTER_COMMA);
			writer.write("\t}");
			writer.write(EOL_DOUBLE);

			writer.write("\t@Override\n");
			writer.write("\tpublic String getTableName() {\n");
			writer.write("\t\treturn tableName");
			writer.write(EOL_WITH_AFTER_COMMA);
			writer.write("\t}");
			writer.write(EOL_DOUBLE);
			writeCheckValues(writer);
			writer.write(EOL_DOUBLE);
			writer.write("}");
			writer.flush();
		} catch (Exception e) {
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
			e.printStackTrace();
		}
		return file;
	}

	private void writeConstructors(BufferedWriter pWriter, String className) throws IOException {
		pWriter.write("\tprivate static String schemaName = \"" + getSchemaName() + "\"");
		pWriter.write(EOL_WITH_AFTER_COMMA);
		pWriter.write("\tprivate static String tableName = \"" + getTableName() + "\"");
		pWriter.write(EOL_WITH_AFTER_COMMA);
		pWriter.write(EOL_DOUBLE);

		pWriter.write("\tpublic " + className + "(){\n");
		pWriter.write("\t\tsuper()");
		pWriter.write(EOL_WITH_AFTER_COMMA);
		pWriter.write("\t\tclassName = \"" + className + "\"");
		pWriter.write(EOL_WITH_AFTER_COMMA);

		if (!keyList.isEmpty()) {
			String comma = "";
			pWriter.write("\t\tsetPrimaryKeys(");
			for (Column column : keyList) {
				pWriter.write(comma + column.getFieldName());
				comma = ", ";
			}
			pWriter.write(")");
			pWriter.write(EOL_WITH_AFTER_COMMA);
			for (Column column : keyList) {
				if (column.isReadonly()) {
					pWriter.write("\t\tsetFieldReadonly(" + column.getFieldName() + ", true)");
					pWriter.write(EOL_WITH_AFTER_COMMA);
				}
			}
		}
		pWriter.write("\t}");

		pWriter.write(EOL_DOUBLE);

		if (!keyList.isEmpty()) {
			String comma = "";
			pWriter.write("\tpublic " + className + "(");
			for (Column column : keyList) {
				pWriter.write(String.format(CONSTRUCTOR_SET1, comma, column.getColumnType(), column.getFunctionName()));
				comma = ", ";
			}
			pWriter.write("){\n");
			pWriter.write("\t\tthis()");
			pWriter.write(EOL_WITH_AFTER_COMMA);
			for (Column column : keyList) {
				pWriter.write(String.format(CONSTRUCTOR_SET2, column.getFieldName(), column.getFunctionName()));
				pWriter.write(EOL_WITH_AFTER_COMMA);
			}

			pWriter.write("\t}");
		}
	}

	private void writeCheckValues(BufferedWriter pWriter) throws IOException {
		pWriter.write("\t@Override\n");
		pWriter.write("\tpublic void checkValues(){\n");
		pWriter.write("\t\tsuper.checkValues();\n");
		for (Column column : columnList) {
			if (!column.getColumnType().endsWith("String"))
				pWriter.write(String.format("\t\tcheck%s(%s);%n", column.getColumnType(), column.getFieldName()));
		}
		pWriter.write("\t}");
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
