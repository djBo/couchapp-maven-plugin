## couchapp-maven-plugin

This maven plugin tries to replicate the functionality found in [CouchApp](https://github.com/couchapp/couchapp). The main reason for it, being cross-platform compatibility during builds.

The basic use-case is a small spring-boot application running as a proxy in front of CouchDB. 

### Goals

The plugin only knows 2 goals, each named for their attached phase:

- `package`<br>Takes your couchapp, and converts it into couchapp.json stored in the target folder
- `deploy`<br>Connects to CouchDB, and uploads couchapp.json as your design document.

### Usage

Add the following to your pom:

```xml
	<packaging>couchapp</packaging>

	<pluginRepositories>
		<pluginRepository>
			<id>couchapp-maven-plugin</id>
			<url>https://raw.github.com/djBo/couchapp-maven-plugin/maven/</url>
		</pluginRepository>
	</pluginRepositories>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugin.couchapp</groupId>
				<artifactId>couchapp-maven-plugin</artifactId>
				<version>1.0.0</version>
				<extensions>true</extensions>
				<configuration>
					...
				</configuration>
			</plugin>
		</plugins>
	</build>
```

### Configuration

**debug** (default: _false_)<br>
Enables debugging

**debug.wire** (default: _false_)<br>
Enables wire debugging

**source** (default: _${project.basedir}/src_)<br>
The source directory of your couchapp

**target** (default: _${project.build.directory}_)<br>
The build target directory

**skip** (default: _false_)<br>
Skips the plugin entirely

**couchdb.scheme** (default: _http_)<br>
CouchDB scheme (http/https)

**couchdb.host** (default: _localhost_)<br>
CouchDB hostname

**couchdb.port** (default: _5984_)<br>
CouchDB port

**couchdb.db**<br>
CouchDB database name

**couchdb.user**<br>
CouchDB username

**couchdb.pass**<br>
CouchDB password
