package horizon.spring.sql;

import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import horizon.base.AbstractComponent;
import horizon.sql.DBAccess;

/**A factory that creates a DBAccess as a bean in the context of the Spring framework.
 * <p>A new DBAccess may or may not use a configuration file, 'dbaccess.xml' by default.<br />
 * To have a DBAccess use a configuration file
 * <ul><li>Specify the 'configLocation' property if the configuration is defined in other location than 'dbaccess.xml'.<br />
 *		   With no value provided for the property, the DBAccess looks up the default 'dbaccess.xml' file.
 *	   </li>
 *	   <li>Specify the 'connectionName' property.</li>
 *	   <li>Do not specify the 'datasource' property.</li>
 * </ul>
 * For a DBAccess to use a DataSource
 * <ul><li>Specify the 'datasource' property.</li>
 *	   <li>Specify the 'sqlsheetLocations' property if necessary.
 *	   </li>
 *	   <li>Do not specify the 'configLocation' and 'connectionName' properties.
 *	   </li>
 * </ul>
 * </p>
 */
public class DBAccessFactoryBean extends AbstractComponent implements FactoryBean<DBAccess>, InitializingBean, ApplicationListener<ApplicationEvent> {
	private String
		configLocation,
		connectionName,
		catalog,
		schema,
		sqlsheetLocations;
	@Autowired
	private ResourceLoader resourceLoader;

	private DataSource datasource;

	/**Sets the name of a configuration defined in the 'dbaccess.xml' file.
	 * @param connectionName name of a configuration defined in the 'dbaccess.xml' file
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	/**Sets the location of the configuration file for DBAccesses.<br />
	 * If you decide to use a configuration file located in an other place than the default 'dbaccess.xml',<br />
	 * specify the location of the configuration file using this method.
	 * @param configLocation location of the configuration file for DBAccesses
	 */
	public void setConfigLocation(String configLocation) {
		this.configLocation = configLocation;
	}

	/**Sets the catalog name of the database.
	 * Use this method to set the catalog name if the JDBC driver does not support {@link Connection#getCatalog()}.
	 * @param catalog catalog name
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	/**Sets the schema name of the database
	 * Use this method to set the schema name if the JDBC driver does not support {@link Connection#getSchema()}.
	 * @param schema schema name
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}

	/**Sets the locations of SQL sheets.
	 * @param sqlsheetLocations locations of SQL sheets
	 */
	public void setSqlsheetLocations(String sqlsheetLocations) {
		this.sqlsheetLocations = sqlsheetLocations;
	}

	/**Sets the datasource for a DBAccess to use.
	 * @param datasource datasource for a DBAccess to use
	 */
	public void setDatasource(DataSource datasource) {
		this.datasource = datasource;
	}

	@Override
	public DBAccess getObject() throws Exception {
		DBAccess dbaccess = new DBAccess()
			.setDatasource(datasource)
			.setConfigLocation(configLocation)
			.setConnectionName(connectionName)
			.setCatalog(catalog)
			.setSchema(schema);
		log().trace(() -> dbaccess + " created");
		return dbaccess;
	}

	@Override
	public Class<DBAccess> getObjectType() {
		return DBAccess.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (datasource == null && isEmpty(connectionName))
			throw new IllegalStateException("Empty or missing property: 'datasource' or 'connectionName'");

		if (datasource != null && !isEmpty(connectionName))
			throw new IllegalStateException("Not allowed to set the 'datasource' and 'connectionName' properties together");

		if (!isEmpty(connectionName) && !isEmpty(sqlsheetLocations))
			throw new IllegalStateException("Not allowed to set the 'connectionName' and 'sqlsheetLocations' properties together");

		if (datasource != null && !isEmpty(configLocation))
			throw new IllegalStateException("Not allowed to set the 'datasource' and 'configLocation' properties together");
	}

	private void configureSQLSheets() {
		if (isEmpty(sqlsheetLocations)) return;
		try {
			Resource[] resources = ResourcePatternUtils
					.getResourcePatternResolver(resourceLoader)
					.getResources(sqlsheetLocations);
			if (isEmpty(resources)) return;

			List<InputStream> sqlsheets = Stream.of(resources)
				.filter(location -> location != null)
				.map(location -> {
					try {
						log().debug(() -> location + " loaded");
						return location.getInputStream();
					} catch (Exception e) {
						throw runtimeException(e);
					}
				})
				.collect(Collectors.toList());
			DBAccess.config().sqlSheetLocations(sqlsheets);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (!(event instanceof ContextRefreshedEvent)) return;

		try {
			afterPropertiesSet();
			configureSQLSheets();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}
}