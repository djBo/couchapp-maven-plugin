package org.apache.maven.plugin.couchapp;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class CouchDbResponse {

	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	private String body;
	private Map<String, String> headers;
	private String reason;
	private int status;

	public final JsonArray asJsonArray() {
		if (body == null || body.trim().isEmpty()) return null;
		try {
			return GSON.fromJson(body, JsonArray.class);
		} catch (JsonSyntaxException e) {
			return null;
		}
	}

	public final JsonObject asJsonObject() {
		if (body == null || body.trim().isEmpty()) return null;
		try {
			return GSON.fromJson(body, JsonObject.class);
		} catch (JsonSyntaxException e) {
			return null;
		}
	}

	public String getBody() {
		return body;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getReason() {
		return reason;
	}

	public int getStatus() {
		return status;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public void setStatus(int status) {
		this.status = status;
	}

}
