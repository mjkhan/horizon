/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

/**Provides classes and facilities to help access to a relational database, or sql database, and data manipulation.
 * <p>To access a database, you use a {@link horizon.sql.DBAccess}.<br />
 * The DBAccess needs a {@link javax.sql.DataSource} or configuration for database connection.
 * </p>
 * <p>With a DBAccess in place, you perform database actions such as query and update on data in the database.<br />
 * Use
 * <ul><li>a {@link horizon.sql.Query} to perform database queries.</li>
 * 	   <li>a {@link horizon.sql.Update} to execute database update</li>
 * 	   <li>a {@link horizon.sql.Batch} to execute database update in batches</li>
 * </ul>
 * The database actions are obtained from the DBAccess<br />
 * and control database connection and transaction automatically.<br />
 * Horizon also offers facilities to have the database actions share the connection and transaction.
 * </p>
 * <p>For detailed information on how to use the provided classes and facilities,<br />
 * please see the example project that is provided along with the distribution.<br />
 * The example project offers
 * <ul><li>tutorial on specific usages of the provided classes and facilities</li>
 * 	   <li>example web application</li>
 * </ul>
 * </p>
 * <h2 id="configuration">Configuration</h2>
 * <p>If you decide to have a DBAccess use a configuration on database connection,<br />
 * define it in an xml file and provide the name of the configuration for the DBAccess.<br />
 * </p>
 * <p>By default, a DBAccess looks up the 'dbaccess.xml' file from classpath.<br />
 * The file can also be in other place.</p>
 * <p>The file must be written in well-formed xml<br />
 * and defines 1 or more configurations of database connection, each identified with the 'name' attribute.<br />
 * Along with configurations of database connection, you can specify locations of SQL sheets if necessary.
 * </p>
 * <p>Typically, the file goes like:
 * <pre><code> {@code <dbaccess>
 * ...
 * </dbaccess>}</code></pre>
 * Inside the root element, specify the configurations of database connection and locations of SQL sheets
 * </p>
 * <h3 id="db-connection-conf">Configurations of database connection</h3>
 * Depending on the connection type, use the following elements to define connection information.
 * <h4>Plain JDBC connection</h4>
 * <pre><code> {@code <jdbc name="unique name of the configuration">
 *     <driver>fully-qualified class name of the JDBC driver</driver>
 *     <url>URL for the database connection</url>
 *     <username>name of the user authorized to access the database</username>
 *     <password>password of the user</passwword>
 * </jdbc>}</code></pre>
 * <h4>DataSource with container-managed transaction</h4>
 * <pre><code> {@code <datasource name="unique name of the configuration">
 *     <jndi-name>JNDI name of the DataSource</jndi-name>
 *     <transactionManagerName>JNDI name of the TransactionManager</transactionManagerName>
 * </datasource>}</code></pre>
 * <h4>DataSource with bean-managed transaction, or {@link javax.transaction.UserTransaction}</h4>
 * <pre><code> {@code <datasource name="unique name of the configuration">
 *     <jndi-name>JNDI name of the DataSource</jndi-name>
 *     <userTransactionName>JNDI name of the UserTransaction</userTransactionName>
 * </datasource>}</code></pre>
 * <h3>SQL sheet locations</h3>
 * Use a {@code <sqlsheets../>} element to specify comma(,)-separated locations of <a href="#sqlsheets">SQL sheets</a>.<br />
 * The locations may be paths on classpath or on file system.<br />
 * <h2 id="sql-parameters">SQL statements and parameters</h2>
 * <p>For database actions, you provide SQL statements for database actions such as a Query, an Update, or a Batch.<br />
 * The statements must be supported by the JDBC driver you employ in your application.
 * </p>
 * <p>In denoting the parameters, you use the '?' character as the JDBC API requires.<br />
 * And provide the arguments matching the number and order of the statements.
 * </p>
 * <p>Horizon also supports named parameters for SQL statements.<br />
 * The parameter name
 * <ul><li>must be alpha-numeric characters starting with an alphabetic character</li>
 * 	   <li>allows no white space characters</li>
 * 	   <li>can be in expression language denoting an object's property.<br />
 * 		   The property name follows the naming convention of JavaBean's property.
 * 	   </li>
 * </ul>
 * With named parameters, you can use a name with reserved tokens as a placeholder for a parameter.<br />
 * The named parameters are resolved and bound to statements at runtime.<br />
 * Horizon supports 2 types of named parameters.
 * </p>
 * <h3>#{parameter-name}</h3>
 * A parameter in the #{param-name} format is at runtime replaced with the '?' character
 * and the argument is resolved and bound appropriately.<br />
 * For example,
 * <pre><code> dbaccess.query()
 *     .sql("SELECT * FROM CUSTOMER WHERE CUST_NAME LIKE CONCAT(<b>#{custName}</b>, '%')")
 *     <b>.param("custName", "Jane")</b>
 *     .getDataset();</code></pre>
 * The "SELECT ..." is translated to
 * <pre><code>SELECT * FROM CUSTOMER WHERE CUST_NAME LIKE CONCAT(?, '%')</code></pre>
 * and "Jane" is bound as an argument to the statement.
 * <h3>${parameter-name}</h3>
 * A parameter in the ${param-name} format is at runtime replaced with the resolved argument.<br />
 * For example,
 * <pre><code> dbaccess.query()
 *     .sql("SELECT * FROM <b>${tableName}</b> WHERE <b>${columnName}</b> = '<b>${columnValue}</b>'")
 *     <b>.param("tableName", "CUSTOMER").param("columnName", "CUST_ID").param("columnValue", "00001")</b>
 *     .getDataset();</code></pre>
 * The "SELECT ..." is translated to
 * <pre><code>SELECT * FROM CUSTOMER WHERE CUST_ID = '00001'</code></pre>
 * <h2 id="sqlsheets">Externalizing (dynamic) SQL statements in SQL Sheets</h2>
 * <p>You can write SQL statements in an SQL sheet<br />
 * and have a Query, an Update, and a Batch look up the required statements<br />
 * to perform database actions.
 * </p>
 * An SQL sheet is a well-formed xml document where you specify
 * <ul><li>namespace with a {@code <sqlsheet namespace="your-namespace">...</sqlsheet>} element.</li>
 * 	   <li>ORM(Object-Relation Mapping) configurations with {@code <orm../>} elements.</li>
 * 	   <li>Instructions for SQL statements(SQL instructions) with {@code <query../>, <update../>, and <sql../>} elements.<br />
 * 		   Note that SQL instructions in an SQL sheet do not support '?'-character parameters for SQL statements.<br />
 * 		   Use only named-parameters for SQL statements in an SQL sheet.
 * 	   </li>
 * 	   <li>Control instructions to support dynamic SQL statements in SQL instructions</li>
 * </ul>
 * A typical SQL sheet consists of the following elements:
 * <pre><code> {@code <sqlsheet namespace="namespace of the sqlsheet">
 *     <orm type="class0" table="table0">...</orm>
 *     <orm type="class1" table="table1">...</orm>
 *
 *     <sql id="sql0">...</sql>
 *     <sql id="sql1">...</sql>
 *
 *     <query id="query0">...
 *        <sql ref="sql0"/>
 *        ...
 *     </query>
 *     <query id="query1">...</query>
 *
 *     <update id="update0">...
 *         <sql ref="sql1"/>
 *         ...
 *     </update>
 *     <update id="update1">...</update>
 * </sqlsheet>}</code></pre>
 * <h3>{@code <sqlsheet../>} element</h3>
 * <p>An {@code <sqlsheet../>} element is a container of SQL instructions and ORM configurations.<br />
 * A file can have only one {@code <sqlsheet../>} element<br />
 * and you must provide a value for the "namespace" attribute of the element.<br />
 * </p>
 * <p>A namespace can span multiple SQL sheets.<br />
 * That is, multiple SQL sheet files share a namespace.
 * </p>
 * <h3>{@code <orm../>} element</h3>
 * <p>An {@code <orm../>} element specifies a configuration for Object-Relation Mapping.<br />
 * You can write as many {@code <orm../>} elements as required in an SQL sheet.
 * </p>
 * <p>A horizon.sql.Query uses an ORM configuration in
 * <ul><li>restoring objects from the results of the getObjects(...) and getObject(...) methods</li>
 * 	   <li>getting an object with the getObject(Class) method</li>
 * </ul>
 * </p>
 * <p>A horizon.sql.Update uses an ORM configuration in creating, updating, and deleting information<br />
 * in the database with the create(Object...), update(Object...), and delete(Object...) methods.
 * </p>
 * <p>For this to work properly, the database table for the objects must be created with primary keys.</p>
 * An {@code <orm../>} element goes as follows:
 * <pre><code> {@code <orm
 *     type="fully-qualified class name. Must be unique"
 *     alias="short name of the class for convenience, Optional. Must be unique"
 *     table="name of the database table where information of the objects is stored, Optional">
 *
 *     <mapping property="name of the object's property" column="column name of the table"/>
 *     <mapping ../>
 *     ...
 *
 *     <!-- Following instructions are optional -->
 *     <beforeInsert
 *         set="comma-separated names of (object's) target properties"
 *         properties="comma-separated names of parameters"/>
 *     <beforeInsert
 *         set="comma-separated names of (object's) target properties"
 *         columns="comma-separated names of query result columns">
 *         SQL instruction that performs query
 *     </beforeInsert>
 *
 *     <beforeUpdate../>
 *
 *     <beforeDelete../>
 * </orm>}</code></pre>
 * <p>The attributes of the {@code <orm../>} and {@code <mapping../>} elements are straightforward and self-explanatory.<br />
 * Note that for the 'property' attribute of the {@code <mapping../>} element, provide a name of an object's property following the naming convention of JavaBean's property.
 * </p>
 * <p>The elements of
 * <ul><li>{@code <beforeInsert../>} for an instruction executed before INSERTing the object information to the database</li>
 * 	   <li>{@code <beforeUpdate../>} for an instruction executed before UPDATEing the object information in the database</li>
 * 	   <li>{@code <beforeDelete../>} for an instruction executed before DELETEing the object information from the database</li>
 * </ul>
 * have the attributes as follows:
 * <ul><li>"set"
 * 		<ul><li>comma(,)-separated names of target properties (of an object) to which named parameters or column values of a query result are set.</li>
 * 		</ul>
 * 	   </li>
 * 	   <li>"properties"
 * 		<ul><li>comma(,)-separated names of source parameters</li>
 * 			<li>Optional</li>
 * 			<li>This attribute must be used along with the "set" attribute.</li>
 * 			<li>The number and order of the parameter names must match with the "set" attribute.</li>
 * 		</ul>
 * 	   </li>
 * 	   <li>"columns"
 * 		<ul><li>comma(,)-separated names of source columns of a query result</li>
 * 			<li>Optional</li>
 * 			<li>The SQL instruction for the query must be specified as an inner content of the element.</li>
 * 			<li>This attribute must be used along with the "set" attribute.</li>
 * 			<li>The number and order of the column names must match with the "set" attribute.</li>
 * 		</ul>
 * 	   </li>
 * </ul>
 * If multiple {@code <beforeXXX ../>} instructions are specified for an {@code <orm../>} configuration,<br />
 * they are executed in the written order.
 * </p>
 * <p>Unlike SQL instructions, ORM configurations are not bound to a namespace.<br />
 * Although specified in an SQL sheet of a namespace,<br />
 * an ORM configuration does not respect the namespace boundary<br />
 * and is good as long as the type and the alias are unique.
 * </p>
 * <h3>SQL instructions</h3>
 * <p>An SQL instruction specifies an SQL statement for database actions to perform.<br />
 * If the JDBC driver supports, you can write as many statements as required.<br />
 * In denoting parameters of a statement, use named parameters.<br />
 * Do not use the '?' character.
 * </p>
 * <h4>{@code <query../>} element</h4>
 * A {@code <query../>} element specifies an instruction to get query results by executing
 * <ul><li>a SELECT statement</li>
 * 	   <li>a stored procedure that returns query results</li>
 * </ul>
 * <p>A horizon.sql.Query uses {@code <query../>} instructions in executing queries.
 * </p>
 * A {@code <query../>} element goes like this:
 * <pre><code> {@code <query id="instruction id" resultType="fully-qualified class name" resultAlias="alias for the class name">}
 * SELECT statement
 * or {call stored-procedure(parameters)}
 * {@code </query>}</code></pre>
 * The attributes of the element are
 * <ul><li>"id": identifier of the instruction unique in the namespace
 * 		<ul><li>For a Query to look up a {@code <query../>} instruction, you should provide the id of the instruction<br />
 * 				in the format "namespace.query-id" for the {@link horizon.sql.Query#sqlId(String)}.
 * 			</li>
 * 			<li>The id must not contain the period(.) character.</li>
 * 		</ul>
 * 	   </li>
 * 	   <li>"resultType": fully-qualified name of the class for each row of the query result, Optional.<br />
 * 		<ul><li>The class must be configured with an {@code <orm../>} element.</li>
 *			<li>Effective only when the query result is returned with {@link horizon.sql.Query#getObjects()} or {@link horizon.sql.Query#getObject()}</li>
 *		</ul>
 * 	   </li>
 * 	   <li>"resultAlias": alias of the name of the class for each row of the query result, Optional.<br />
 * 		<ul><li>The class must be configured with an {@code <orm../>} element.</li>
 *			<li>Effective only when the query result is returned with {@link horizon.sql.Query#getObjects()} or {@link horizon.sql.Query#getObject()}</li>
 *		</ul>
 * 	   </li>
 * </ul>
 * <h4>{@code <update../>} element</h4>
 * An {@code <update../>} element specifies an instruction to execute
 * <ul><li>INSERT, UPDATE, or DELETE statements</li>
 * 	   <li>a stored procedure that saves information</li>
 * </ul>
 * <p>A horizon.sql.Update and a horizon.sql.Batch use {@code <update../>} instructions in creating, updating, and deleting
 * object information in a database.
 * </p>
 * An {@code <update../>} element goes like this:
 * <pre><code> {@code <update id="instruction id">}
 * INSERT, UPDATE, or DELETE statement
 * or {call stored-procedure(parameters)}
 *     <!-- Following instructions are optional -->
 *     {@code <before
 *         set="comma-separated names of (object's) target properties"
 *         properties="comma-separated names of parameters"/>
 *     <before
 *         set="comma-separated names of (object's) target properties"
 *         columns="comma-separated names of query result columns">
 *         SQL instruction that performs query
 *     </before>}
 * {@code </update>}</code></pre>
 * The attribute of the element is
 * <ul><li>"id": identifier of the instruction unique in the namespace
 * 		<ul><li>For an Update or a Batch to look up an {@code <update../>} instruction, you should provide the id of the instruction<br />
 * 				in the format "namespace.update-id" for the {@link horizon.sql.Update#sqlId(String)} or the {@link horizon.sql.Batch#sqlId(String)}.
 * 			</li>
 * 			<li>The id must not contain the period(.) character.</li>
 * 		</ul>
 * 	   </li>
 * </ul>
 * The {@code <before../>} elements are executed before the {@code <update../>} instruction<br />
 * and have the same attributes as the {@code <beforeXXX../>} elements of an {@code <orm../>} element<br />
 * and behave likewise as those elements.
 * <h4>{@code <sql../>} element</h4>
 * <p>An {@code <sql../>} element specifies an SQL instruction that is shared by other instructions like {@code <query../>} and {@code <update../>}.<br />
 * With an {@code <sql../>} element, you specify
 * <ul><li>SELECT, INSERT, UPDATE, DELETE statement</li>
 * 	   <li>a stored procedure</li>
 * </ul>
 * The element allows no {@code <beforeXXX../>} instructions.
 * </p>
 * <p>To declare an {@code <sql../>} instruction, it goes like:
 * <pre><code> {@code <sql id="instruction id">}
 * SELECT, INSERT, UPDATE, DELETE statement
 * or {call stored-procedure(parameters)}
 * {@code </sql>}</code></pre>
 * The 'id' attribute is for an identifier unique in the namespace and allows no period(.) character.
 * </p>
 * <p>To refer to the {@code <sql../>} instruction:
 * <pre><code> {@code <query id="query0">
 *     ...
 *     <sql ref="reference to the id of an sql instruction"/>
 *     ...
 * </query>}</code></pre>
 * The 'ref' attribute specifies an id of an sql instruction defined elsewhere and included in the referring element.<br />
 * By default, it looks up the specified sql instruction in the namespace of the containing element.<br />
 * To have it look up an sql instruction in other namespace, specify the ref attribute with a namespace as follows.
 * <pre><code> {@code <query id="query0">
 *     ...
 *     <sql ref="other-namespace.reference to the id of an sql instruction"/>
 *     ...
 * </query>}</code></pre>
 * </p>
 * <h4>Control instructions for dynamic SQL statements</h4>
 * <p>Control instructions are to generate SQL statements dynamically from such SQL instructions as {@code <query../>}, {@code <update../>}, and {@code <sql../>}.<br />
 * Nested in the SQL instructions, they evaluate the properties and/or named parameters given at runtime
 * to generate an SQL statement.</br>
 * There are 2 types of control instructions,<br />
 * the one for conditional instruction and the other for loop instruction.
 * </p>
 * <h5>{@code <if../>} element</h5>
 * An {@code <if../>} element specifies a conditional instruction that
 * <ul><li>when the given condition evaluates to be true,</li>
 * 	   <li>adds its inner instruction to the parent instruction.</li>
 * </ul>
 * An {@code <if../>} element goes like:
 * <pre><code> {@code <if test="expression language that returns a boolean value">
 * SQL statements or another control instructions
 * </if>}</code></pre>
 * <ul><li>The "test" attribute is an expression language<br />
 * 			that evaluates the given parameters and/or properties and returns a boolean value</li>
 * 	   <li>The inner instruction can be
 * 		<ul><li>SQL statement</li>
 * 			<li>{@code <sql../> instruction reference}</li>
 * 			<li>yet another control instruction</li>
 * 		</ul>
 * 	   </li>
 * </ul>
 * <h5>{@code <foreach../>} element</h5>
 * A {@code <foreach../>} element specifies an instruction that
 * <ul><li>accepts an {@link java.lang.Iterable}, an array, or a {@link java.util.Map} as a parameter</li>
 * 	   <li>loops over a given parameter or property</li>
 * 	   <li>evaluates each element value</li>
 * 	   <li>adds its inner instruction to the parent instruction.</li>
 * </ul>
 * A {@code <foreach../>} element goes like:
 * <pre><code> {@code <foreach
 *     items="name of a parameter or property"
 *     var="variable name for each item"
 *     separator="string that separates instructions for each item"
 *     index="variable name for the index value">
 * SQL statements or another control instructions
 * </foreach>}</code></pre>
 * <ul><li>The "items" attribute is a name of a parameter or property.<br />
 * 		   The value of the parameter or property must be either an Iterable or an array.
 * 	   </li>
 * 	   <li>The "var" attribute is the name of a variable that holds each item of the items.<br />
 * 		   It is used as a named parameter in the inner instruction.
 * 	   </li>
 * 	   <li>The "separator" attribute is a string that separates instructions for each item.
 * 	   </li>
 * 	   <li>The "index" attribute is the name of a variable that holds the index of each item.<br />
 * 		   The index value starts from 0.<br />
 * 		   It is used as a named parameter in the inner instruction.<br />
 * 		   Optional.
 * 	   </li>
 * 	   <li>The inner instruction can be
 * 		<ul><li>SQL statement</li>
 * 			<li>{@code <sql../> instruction reference}</li>
 * 			<li>yet another control instruction</li>
 * 		</ul>
 * 	   </li>
 * </ul>
 * Following is an example of a {@code <foreach../>} instruction.
 * <pre><code> {@code <update id="update0">}
 * UPDATE CUSTOMER
 * SET CREDIT = CASE CUST_ID
 *     {@code <foreach items="custIDs" var="custID" separator=" " index="index">}WHEN #{custID} THEN ((#{index} + 1) * 10000)
 *     {@code </foreach>} ELSE CUST_ID END
 * WHERE CUST_ID IN ({@code <foreach items="custIDs" var="custID" separator=" ,">}#{custID}{@code </foreach>})
 * {@code </update>}</code></pre>
 * <h2>Acknowledgement</h2>
 * The ideas of
 * <ul><li>externalizing SQL statements</li>
 * 	   <li>dynamic SQL statements</li>
 * </ul>
 * are inspired by <a href="https://mybatis.org" target="_blank">MyBatis</a>.
 */
package horizon.sql;