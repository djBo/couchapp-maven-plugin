package org.apache.maven.plugin.couchapp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class CouchAppMojo extends AbstractMojo {

	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	private CouchDbClient client;

	/**
	 * Debug logging
	 */
	@Parameter(property = "debug", defaultValue = "false")
	protected boolean debug;

	/**
	 * Debug wire logging
	 */
	@Parameter(property = "debug.wire", defaultValue = "false")
	protected boolean debugWire;

	/**
	 * The source directory
	 */
	@Parameter(property = "source", defaultValue = "${project.basedir}/src")
	protected String source;

	/**
	 * The target directory
	 */
	@Parameter(property = "target", defaultValue = "${project.build.directory}")
	protected String target;

	/**
	 * Skip generation
	 */
	@Parameter(property = "skip", defaultValue = "false")
	protected boolean skip;

	/**
	 * CouchDB Scheme
	 */
	@Parameter(property = "couchdb.scheme", defaultValue = "http")
	protected String couchdbScheme;

	/**
	 * CouchDB Host
	 */
	@Parameter(property = "couchdb.host", defaultValue = "localhost")
	protected String couchdbHost;

	/**
	 * CouchDB Port
	 */
	@Parameter(property = "couchdb.port", defaultValue = "5984")
	protected Integer couchdbPort;

	/**
	 * CouchDB Database
	 */
	@Parameter(property = "couchdb.db", defaultValue = "")
	protected String couchdbDb;

	/**
	 * CouchDB User
	 */
	@Parameter(property = "couchdb.user", defaultValue = "")
	protected String couchdbUser;

	/**
	 * CouchDB Pass
	 */
	@Parameter(property = "couchdb.pass", defaultValue = "")
	protected String couchdbPass;

	protected CouchDbClient getClient() {
		if (client == null) {
			client = new CouchDbClient(getResourceUrl(), debug);
		}
		return client;
	}

	protected String getResourceUrl() {
		if (isEmpty(couchdbUser) || isEmpty(couchdbPass)) {
			if (hasDefaultPort()) {
				return couchdbScheme + "://" + couchdbHost + "/" + couchdbDb;
			} else {
				return couchdbScheme + "://" + couchdbHost + ":" + couchdbPort + "/" + couchdbDb;
			}
		} else {
			if (hasDefaultPort()) {
				return couchdbScheme + "://" + couchdbUser + ":" + couchdbPass + "@" + couchdbHost + "/" + couchdbDb;
			} else {
				return couchdbScheme + "://" + couchdbUser + ":" + couchdbPass + "@" + couchdbHost + ":" + couchdbPort + "/" + couchdbDb;
			}
		}
	}

	protected boolean hasDefaultPort() {
		return ("http".contentEquals(couchdbScheme) && (couchdbPort == 80)) || ("https".contentEquals(couchdbScheme) && couchdbPort == 443);
	}

	protected void setDebug(final boolean debug) {
		this.debug = debug;
	}

	protected void setDebugWire(final boolean debugWire) {
		this.debugWire = debugWire;
	}

	/*
	 * https://stackoverflow.com/a/20940906/553317
	 */
	protected void setResourceUrl(final String url) {
		if (url == null) {
			getLog().info("Resource uri: " + getResourceUrl());
			return;
		}
		try {
			final URI result = new URI(url);
			couchdbScheme = result.getScheme();
			couchdbHost = result.getHost();
			couchdbPort = result.getPort();
			couchdbDb = result.getPath();
			if (couchdbDb.startsWith("/")) couchdbDb = couchdbDb.substring(1);
			if (result.getUserInfo() != null) {
				final String[] auth = result.getUserInfo().split(":");
				if (auth.length == 2) {
					couchdbUser = auth[0];
					couchdbPass = auth[1];
				}
			}
			//couchDbAuth = base64encode(result.getUserInfo());
			getLog().info("Resource uri: " + url);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	protected void setSkip(final boolean skip) {
		this.skip = skip;
	}

	protected void setSource(final String source) {
		this.source = source;
	}

	protected void setTarget(final String target) {
		this.target = target;
	}

	protected void checkCouchAppResource(final File source) throws IOException {
		final File couchappResource = new File(source.toString(), ".couchapprc");
		if (couchappResource.isFile()) {
			getLog().info("Loading couchapp resource configuration...");
			setResourceUrl(getStringFromJson(readFileAsJsonObject(couchappResource), "env", "default", "db"));
		} else {
			setResourceUrl(null);
		}
	}

	protected File checkSource() {
		final File result = new File(source);
		if (!result.isDirectory()) {
			getLog().info("sourceFolder does not exist, skipping.");
			return null;
		}
		return result;
	}

	protected File checkTarget() {
		if (target == null) {
			return null;
		}
		final File result = new File(target);
		if (!result.isDirectory()) {
			getLog().info("targetFolder does not exist, creating...");
			result.mkdirs();
		}
		return result;
	}

	protected void debug() throws MojoExecutionException, MojoFailureException {
		if (debug) {
			getLog().debug("Configuration:");
			getLog().debug("debug: " + debug);
			getLog().debug("debug.wire: " + debugWire);
			getLog().debug("skip: " + skip);
			getLog().debug("source: " + source);
			getLog().debug("target: " + target);
			getLog().debug("couchdb.scheme: " + couchdbScheme);
			getLog().debug("couchdb.host: " + couchdbHost);
			getLog().debug("couchdb.port: " + couchdbPort);
			getLog().debug("couchdb.db: " + couchdbDb);
			getLog().debug("couchdb.user: " + couchdbUser);
			getLog().debug("couchdb.pass: " + couchdbPass);
		}
	}

	protected void initialize() {
		if (debugWire) {
			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
			System.setProperty("log4j.category.org.apache.http.wire", "DEBUG");
			//System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "INFO");
			System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "debug");
		}
	}

	public static String base64decode(final String string) {
		return new String(Base64.decodeBase64(string), StandardCharsets.UTF_8);
	}

	public static String base64encode(final byte[] bytes) {
		return Base64.encodeBase64String(bytes);
	}

	public static String base64encode(final String string) {
		return base64encode(string.getBytes(StandardCharsets.UTF_8));
	}

	public static boolean isEmpty(final String string) {
		return (string == null || string.trim().isEmpty());
	}

	public static String md5(final byte[] bytes) throws IOException {
		try {
			final MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(bytes);
			return DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static JsonObject getJsonFromMap(Map<String, String> map) {
		final JsonObject result = new JsonObject();
		for (Entry<String, String> entry : map.entrySet()) {
			result.addProperty(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static JsonObject getJsonFromString(final String string) {
		return GSON.fromJson(string, JsonObject.class);
	}

	public static String getStringFromJson(final JsonObject object, String... paths) {
		JsonObject result = object;
		for (int i = 0; i < paths.length - 1; i++) {
			result = result.get(paths[i]).getAsJsonObject();
		}
		return result.get(paths[paths.length - 1]).getAsString();
	}

	public static String prettyPrint(final JsonElement element) {
		return GSON.toJson(element);
	}

	public static JsonArray readFileAsJsonArray(final File file) throws IOException {
		return GSON.fromJson(readFileAsString(file), JsonArray.class);
	}

	public static JsonObject readFileAsJsonObject(final File file) throws IOException {
		return getJsonFromString(readFileAsString(file));
	}

	public static String readFileAsString(final File file) throws IOException {
		return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
	}

}
