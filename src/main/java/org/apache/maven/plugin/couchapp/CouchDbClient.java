package org.apache.maven.plugin.couchapp;

import static org.apache.maven.plugin.couchapp.CouchAppMojo.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

public class CouchDbClient {

	private final Log log = new SystemStreamLog();
	private final CloseableHttpClient client = HttpClients.createDefault();

	private boolean debug = false;

	private String couchDbScheme;
	private String couchDbHost;
	private Integer couchDbPort;
	private String couchDbAuth;
	private String couchDb;

	public CouchDbClient(final String resourceURI) {
		this(resourceURI, false);
	}

	public CouchDbClient(final String resourceURI, final boolean debug) {
		this.debug = debug;
		decomposeURI(resourceURI);
	}

	public String getCouchDb() {
		return couchDbScheme + "://" + couchDbHost + ":" + couchDbPort + "/" + couchDb;
	}

	public void setDebug(final boolean debug) {
		this.debug = debug;
	}

	public CouchDbResponse checkForDatabase() throws IOException {
		return performRequest(new HttpHead(getCouchDb()));
	}

	public CouchDbResponse createDatabase() throws IOException {
		return performRequest(new HttpPut(getCouchDb()));
	}

	public CouchDbResponse getDesignDocument(final String name) throws  IOException {
		return performRequest(new HttpGet(getCouchDb() + "/" + name));
	}

	public CouchDbResponse updateDesignDocument(final String name, final String body) throws IOException {
		final HttpPut request = new HttpPut(getCouchDb() + "/" + name);
		request.setEntity(new StringEntity(body));
		request.setHeader("Content-Type", "application/json");
		return performRequest(request);
	}

	private CouchDbResponse performRequest(final HttpRequestBase request) throws IOException {
		if (debug) log.debug("Request: " + request.getMethod() + " " + request.getURI());
		final CouchDbResponse result = new CouchDbResponse();
		request.addHeader("Accept", "application/json");
		request.addHeader("User-Agent", "couchapp/1.0.0-SNAPSHOT");
		request.addHeader("Authorization", "Basic " + couchDbAuth);
		if (debug) log.debug("Headers: " + getJsonFromMap(readHeaders(request)));
		final CloseableHttpResponse response = client.execute(request);
		try {
			result.setStatus(response.getStatusLine().getStatusCode());
			result.setReason(response.getStatusLine().getReasonPhrase());
			if (debug) log.debug("Got response: " + result.getStatus() + " " + result.getReason());
			if (response.getEntity() != null) {
				result.setBody(readResponse(response.getEntity().getContent()));
			}
			result.setHeaders(readHeaders(response));
			if (debug) log.debug("Headers: " + getJsonFromMap(result.getHeaders()));
		} finally {
			response.close();
		}
		return result;
	}

	/*
	 * https://stackoverflow.com/a/20940906/553317
	 */
	private void decomposeURI(final String uri) {
		try {
			final URI result = new URI(uri);
			couchDbScheme = result.getScheme();
			couchDbHost = result.getHost();
			couchDbPort = result.getPort();
			couchDbAuth = base64encode(result.getUserInfo());
			couchDb = result.getPath();
			if (couchDb.startsWith("/")) couchDb = couchDb.substring(1);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, String> readHeaders(final HttpMessage message) {
		final Map<String, String> result = new HashMap<>();
		for (Header header : message.getAllHeaders()) {
			result.put(header.getName(), header.getValue());
		}
		return result;
	}

	private String readResponse(final InputStream in) throws IOException {
		final StringBuffer result = new StringBuffer();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
		} finally {
			reader.close();
		}
		return result.toString();
	}

}
