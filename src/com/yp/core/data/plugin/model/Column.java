package com.yp.core.data.plugin.model;

import java.util.Locale;

public class Column {
	public static final Locale LOCALE_EN = new Locale("en", "US");
	private static final String DATE = "DATE";
	private static final String DATETIME = "DATETIME";

	private String columnName, fieldName, functionName, columnType, columnTypeFullName;
	private boolean isKey, isAutoIncrement, isReadonly, isDate;

	public Column(String pColumnName, String pColumnType, boolean pIsAutoIncrement) {
		super();
		setColumnName(pColumnName);
		setColumnTypeFullName(pColumnType);
		isAutoIncrement = pIsAutoIncrement;
		isReadonly = false;
		isDate = false;
	}

	public String getColumnName() {
		return columnName;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setColumnName(String pColumnName) {
		if (!isNull(pColumnName)) {
			pColumnName = pColumnName.trim();
			columnName = pColumnName;
			fieldName = pColumnName.toUpperCase(LOCALE_EN);
			functionName = ucaseFirsChar(pColumnName);
			isDate = pColumnName.endsWith(DATE) || pColumnName.endsWith(DATETIME);
		}
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType(String pColumnType) {
		columnType = pColumnType;
	}

	public String getColumnTypeFullName() {
		return columnTypeFullName;
	}

	// exract type from toString
	public void setColumnTypeFullName(String pColumnTypeFullName) {
		columnTypeFullName = pColumnTypeFullName;
		int i = pColumnTypeFullName.lastIndexOf('.') + 1;
		if (i > 0)
			columnType = pColumnTypeFullName.substring(i);
		else
			columnType = pColumnTypeFullName;
	}

	public boolean isKey() {
		return isKey;
	}

	public void setKey(boolean pIsKey) {
		isKey = pIsKey;
	}

	public boolean isAutoIncrement() {
		return isAutoIncrement;
	}

	public void setAutoIncrement(boolean pIsAutoIncrement) {
		isAutoIncrement = pIsAutoIncrement;
	}

	public boolean isReadonly() {
		return isReadonly;
	}

	public void setReadonly(boolean pIsReadonly) {
		isReadonly = pIsReadonly;
	}

	public boolean isDate() {
		return isDate;
	}

	public void setDate(boolean pIsDate) {
		isDate = pIsDate;
	}

	public static String ucaseFirsChar(String pString) {
		StringBuilder sb = new StringBuilder();
		if (!isNull(pString)) {
			String[] temp = pString.split("_");
			for (String s : temp) {
				sb.append(s.substring(0, 1).toUpperCase(LOCALE_EN));
				sb.append(s.substring(1).toLowerCase(LOCALE_EN));
			}
		}
		return sb.toString();
	}

	public static boolean isNull(String pString) {
		return pString == null || "".equals((pString = pString.trim())) || "null".equals(pString)
				|| "undefined".equals(pString);
	}
}
