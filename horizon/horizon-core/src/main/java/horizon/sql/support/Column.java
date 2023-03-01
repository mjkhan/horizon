/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import horizon.base.AbstractComponent;

/**Meta information on a Table's column<br />.
 * A Column describes a Table's column  with information that follows:
 * <ul><li>the column's {@link #name() name}</li>
 * 	   <li>whether the column's value is {@link #isKey() part of identifiers}</li>
 * 	   <li>whether the column's value is {@link #isAutoIncrement() auto-incremented}</li>
 * </ul>
 */
public class Column extends AbstractComponent implements Serializable {
	private static final long serialVersionUID = 1L;

	private String name;
	private boolean
		key,
		autoIncrement;

	/**Returns the Column's name.
	 * @return the Column's name
	 */
	public String name() {
		return name;
	}

	/**Sets the Column's name
	 * @param name the Column's name
	 */
	public Column setName(String name) {
		this.name = notEmpty(name, "name");
		return this;
	}

	/**Returns whether a column's value is part of identifier.
	 * @return
	 * <ul><li>true if a column's value is part of identifier</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean isKey() {
		return key;
	}

	/**Sets whether a column's value is part of identifier.
	 * @param key
	 * <ul><li>true to set the column's value as part of identifier</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public Column setKey(boolean key) {
		this.key = key;
		return this;
	}

	/**Returns whether a column's value is auto-incremented.
	 * @return
	 * <ul><li>true if a column's value is auto-incremented</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	/**Sets whether a column's value is auto-incremented.
	 * @param autoIncrement
	 * <ul><li>true to set a column's value is auto-incremented</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public Column setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
		return this;
	}

	@Override
	public String toString() {
		return "Column{name=\"" + name + "\", key=" + key + ", autoIncrement=" + autoIncrement + "}";
	}

	static final RuntimeException notFound(String columnName) {
		return new RuntimeException("Column not found named '" + columnName + "'");
	}

	public static class Token {
		public static Token create(Column column) {
			return new Token().setColumnName(column.name);
		}

		public static List<Token> create(List<Column> columns) {
			return columns.stream().map(Token::create).collect(Collectors.toList());
		}

		private String
			columnName,
			token;

		/**Returns the columnName.
		 * @return the columnName
		 */
		public String getColumnName() {
			return columnName;
		}

		/**Sets the columnName.
		 * @param columnName the columnName to set
		 * @return the Token
		 */
		public Token setColumnName(String columnName) {
			this.columnName = columnName;
			return this;
		}

		/**Returns the token.
		 * @return the token
		 */
		public String getToken() {
			return ifEmpty(token, () -> "?");
		}

		/**Sets the token.
		 * @param token the token to set
		 * @return the Token
		 */
		public Token setToken(String token) {
			this.token = token;
			return this;
		}
	}
}