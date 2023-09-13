/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.w3c.dom.Element;

import horizon.base.AbstractComponent;
import horizon.jndi.ObjectLocator;
import horizon.sql.support.SQLBuilder;
import horizon.util.Xmlement;

class DatasourceFactory extends AbstractComponent {
	static final String DEFAULT_LOCATION = "dbaccess.xml";
	private static final HashMap<String, DatasourceFactory> cache = new HashMap<>();
	private String path;
	private HashMap<String, Config> configs;

	static DatasourceFactory get(String configLocation) {
		String location = ifEmpty(configLocation, () -> DEFAULT_LOCATION);
		DatasourceFactory factory = cache.get(location);

		if (factory == null) {
			cache.put(location, factory = new DatasourceFactory());
			factory.configure(location);
		}
		return factory;
	}

	static DataSource get(String configLocation, String connectionName) {
		DatasourceFactory factory = get(configLocation);
		return factory.create(connectionName);
	}

	static Config config(String connectionName) {
		for (DatasourceFactory factory: cache.values()) {
			Config config = factory.configs.get(connectionName);
			if (config != null)
				return config;
		}
		return null;
	}

	static void setMetaInfo(String connectionName, Connection conn) throws SQLException {
		Config conf = config(connectionName);
		if (conf != null && !conf.hasMeta())
			conf.setMetaInfo(conn.getMetaData());
	}

	private void configure(String path) {
		this.path = path;
		configure(Xmlement.get().getDocument(path));
	}

	private void configure(Element doc) {
		String s = Xmlement.get().childContent(doc, "sqlsheets");
		if (!isEmpty(s))
			SQLBuilder.loadSQLSheets(s.split(","));
		if (configs == null)
			configs = new HashMap<>();
		else
			configs.clear();

		JDBConnection.Conf.create(doc).forEach(conf -> configs.put(conf.getName(), conf));
		JNDIConnection.Conf.create(doc).forEach(conf -> configs.put(conf.getName(), conf));
	}

	DataSource create(String name) {
		Config config = configs.get(name);
		if (config == null)
			throw new NullPointerException("Database configuration named '" + name + "' not found in " + path);
		return config.create();
	}

	static void clear() {
		if (!isEmpty(cache)) {
			cache.clear();
			log(DatasourceFactory.class).trace(() -> "DBAccess configurations cleared");
		}
	}

	static Transaction.Definition getTransactionDefinition(DataSource datasource) {
		Transaction.Definition def = new Transaction.Definition();
		Class<?> klass = datasource.getClass();
		if (JNDIConnection.class.equals(klass)) {
			JNDIConnection jndiConnection = (JNDIConnection)datasource;
			String jndiName = jndiConnection.getTransactionManagerName();
			if (!isEmpty(jndiName))
				return def.setType(Transaction.Type.CMT)
						  .setJndiName(jndiName);

			jndiName = jndiConnection.getUserTransactionName();
			if (!isEmpty(jndiName))
				return def.setType(Transaction.Type.BMT)
						  .setJndiName(jndiName);
		}
		return def.setType(Transaction.Type.JDBC);
	}

	static abstract class Config {
		private String
			name,
			dbmsProductName,
			dbmsProductVersion,
			driverName,
			driverVersion,
			catalog,
			schema;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		private boolean hasMeta() {
			return !isEmpty(dbmsProductName);
		}

		private void setMetaInfo(DatabaseMetaData metaData) throws SQLException {
			dbmsProductName = metaData.getDatabaseProductName();
			dbmsProductVersion = metaData.getDatabaseProductVersion();
			driverName = metaData.getDriverName();
			driverVersion = metaData.getDriverVersion();
		}

		public String getDbmsProductName() {
			return dbmsProductName;
		}

		public String getDbmsProductVersion() {
			return dbmsProductVersion;
		}

		public String getDriverName() {
			return driverName;
		}

		public String getDriverVersion() {
			return driverVersion;
		}

		/**Returns the catalog.
		 * @return the catalog
		 */
		public String getCatalog() {
			return catalog;
		}

		/**Sets catalog to the catalog.
		 * @param catalog the catalog to set
		 */
		public void setCatalog(String catalog) {
			this.catalog = catalog;
		}

		/**Returns the schema.
		 * @return the schema
		 */
		public String getSchema() {
			return schema;
		}

		/**Sets schema to the schema.
		 * @param schema the schema to set
		 */
		public void setSchema(String schema) {
			this.schema = schema;
		}

		public abstract DataSource create();
	}
}

class JDBConnection extends AbstractComponent implements DataSource {
	private String
		driver,
		url,
		username,
		password;

	JDBConnection setDriver(String driver) {
		this.driver = driver;
		return this;
	}

	JDBConnection setUrl(String url) {
		this.url = url;
		return this;
	}

	JDBConnection setUsername(String username) {
		this.username = username;
		return this;
	}

	JDBConnection setPassword(String password) {
		this.password = password;
		return this;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(username, password);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		try {
			Class.forName(driver);
			return DriverManager.getConnection(url, username, password);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	private static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw unsupported();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw unsupported();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw unsupported();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw unsupported();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw unsupported();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw unsupported();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw unsupported();
	}

	static class Conf extends DatasourceFactory.Config {
		static List<DatasourceFactory.Config> create(Element doc) {
			Xmlement xml = Xmlement.get();
			ArrayList<DatasourceFactory.Config> result = new ArrayList<>();
			for (Element jdbc: xml.getChildren(doc, "jdbc")) {
				Conf conf = new Conf();
				conf.setName(xml.attribute(jdbc, "name"));
				conf.setDriver(xml.childContent(jdbc, "driver"))
					.setUrl(xml.childContent(jdbc, "url"))
					.setUsername(xml.childContent(jdbc, "username"))
					.setPassword(xml.childContent(jdbc, "password"));
				conf.setCatalog(xml.childContent(jdbc, "catalog"));
				conf.setSchema(xml.childContent(jdbc, "schema"));
				result.add(conf);
			}
			return result;
		}

		private String
			driver,
			url,
			username,
			password;
		public Conf setDriver(String driver) {
			this.driver = driver;
			return this;
		}

		public Conf setUrl(String url) {
			this.url = url;
			return this;
		}

		public Conf setUsername(String username) {
			this.username = username;
			return this;
		}

		public Conf setPassword(String password) {
			this.password = password;
			return this;
		}

		@Override
		public JDBConnection create() {
			return new JDBConnection()
				.setDriver(driver)
				.setUrl(url)
				.setUsername(username)
				.setPassword(password);
		}
	}
}

class JNDIConnection extends AbstractComponent implements DataSource {
	private String
		jndiName,
		transactionManagerName,
		userTransactionName;
	private DataSource datasource;

	JNDIConnection setJndiName(String jndiName) {
		this.jndiName = jndiName;
		return this;
	}

	String getTransactionManagerName() {
		return transactionManagerName;
	}

	JNDIConnection setTransactionManagerName(String transactionManagerName) {
		this.transactionManagerName = transactionManagerName;
		return this;
	}

	String getUserTransactionName() {
		return userTransactionName;
	}

	JNDIConnection setUserTransactionName(String userTransactionName) {
		this.userTransactionName = userTransactionName;
		return this;
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (datasource == null) {
			datasource = ObjectLocator.get(null).lookup(notEmpty(jndiName, "jndiName"));
		}
		return datasource.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

	private static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw unsupported();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw unsupported();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw unsupported();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw unsupported();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw unsupported();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw unsupported();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw unsupported();
	}

	static class Conf extends DatasourceFactory.Config {
		static List<DatasourceFactory.Config> create(Element doc) {
			Xmlement xml = Xmlement.get();
			ArrayList<DatasourceFactory.Config> result = new ArrayList<>();
			for (Element datasource: xml.getChildren(doc, "datasource")) {
				Conf conf = new Conf();
				conf.setName(xml.attribute(datasource, "name"));
				conf.jndiName = xml.childContent(datasource, "jndi-name");
				conf.transactionManagerName = xml.childContent(datasource, "transactionManagerName");
				conf.userTransactionName = xml.childContent(datasource, "userTransactionName");
				conf.setCatalog(xml.childContent(datasource, "catalog"));
				conf.setSchema(xml.childContent(datasource, "schema"));
				result.add(conf);
			}
			return result;
		}

		private String
			jndiName,
			transactionManagerName,
			userTransactionName;

		@Override
		public JNDIConnection create() {
			return new JNDIConnection()
				.setJndiName(jndiName)
				.setTransactionManagerName(transactionManagerName)
				.setUserTransactionName(userTransactionName);
		}
	}
}