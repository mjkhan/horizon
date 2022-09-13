<h1>What is Horizon?</h1>
<p>Horizon is a Java class library that helps developers write applications using relational database(or SQL database) technology.<br>
And it also provides a JavaScript library to help write HTML pages using data from backend components written using Horizon classes.
</p>
<h1>Features</h1>
<h4>Well-known data objects</h4>
<p>With a family of data objects that is well known to and used heavily by the library's other features,<br>
you do not have to define different data objects for different entity classes.<br>
It provides Horizon and applications using it with an opportunity to simplify and automate the required features.
</p>
<h4>Easy access to relational database</h4>
<p>Horizon offers objects wrapping the access to a relational database.<br>
In accessing the database
<ul><li>the connection and the transaction are controlled easily.</li>
	<li>SQL statement execution is straightforward.</li>
	<li>Named parameters are supported in SQL statement execution.</li>
	<li>You can externalize SQL statements in SQL sheet files.</li>
</ul>
</p>
<h4>Object persistence</h4>
<p>It is a light weight framework to save and load entity objects to and from a relational database.<br>
Leveraging the ORM information you define in SQL sheets,
you can load, create, update, and delete information of objects in the database.<br />
You do not have to have your objects implement any particular interfaces. 
</p>
<h4>Hierarchy framework</h4>
<p>Using the mini framework for hierarchical objects, you can restore those objects of hierarchical structure.<br />
You do not have to have your objects implement any particular interfaces. 
</p>
<h1>Getting Started</h1>
<h4 id="requirements">Requirements</h4>
Horizon works
<ul><li>in Java 8 or later</li>
	<li>with API implementations of
		<ul><li>SLF4J</li>
			<li>JDBC</li>
			<li>Java Transaction</li>
			<li>Java Expression Language</li>
		</ul>
	</li>
</ul>
Horizon-spring works with
<ul><li>spring-context-5.x.x.RELEASE or later</li>
	<li>spring-jdbc-5.x.x.RELEASE or later</li>
	<li>spring-tx-5.x.x.RELEASE or later</li>
</ul>
Note that in production environment where a Horizon application is deployed to an application server,<br />
please see to it whether it already has implementations of dependent APIs available.
<h4 id="installation">Installation</h4>
To install Horizon
<ul><li>Unpack the distribution to any place that suits your need.</li>
	<li>Add to the CLASSPATH <code>horizon-core-22.10.01.jar</code> and, if necessary, <code>horizon-spring-22.10.01.jar</code> in the <code>lib</code> directory.</li>
	<li>Write configurations such as dbaccess.xml and/or SQL sheets and add them to the CLASSPATH.</li>
</ul>
To install Horizon into your local maven repository
<ul><li>Move to the 'lib' directory of Horizon.</li>
	<li>Install the parent module of Horizon by executing
		<pre><code>mvn install:install-file -Dpackaging=pom -Dfile=horizon-22.10.01.pom -DpomFile=horizon-22.10.01.pom</code></pre>
	</li>
	<li>Install horizon-core by executing
		<pre><code>mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=horizon-core-22.10.01.jar</code></pre>
	</li>
	<li>Install horizon-spring by executing
		<pre><code>mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=horizon-spring-22.10.01.jar</code></pre>
	</li>
</ul>
The maven coordinates of horizon-core is
<pre><code>	&lt;groupId>horizon&lt;/groupId>
	&lt;artifactId>horizon-core&lt;/artifactId>
	&lt;version>22.10.01&lt;/version></code></pre>
And the maven coordinates of horizon-spring is
<pre><code>	&lt;groupId>horizon&lt;/groupId>
	&lt;artifactId>horizon-spring&lt;/artifactId>
	&lt;version>22.10.01&lt;/version></code></pre>
