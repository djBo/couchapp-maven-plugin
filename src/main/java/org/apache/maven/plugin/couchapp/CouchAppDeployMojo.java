package org.apache.maven.plugin.couchapp;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.gson.JsonObject;

/**
 * Upload a couchapp to CouchDB
 *
 */
@Mojo(
		name = "deploy",
		defaultPhase = LifecyclePhase.DEPLOY,
		requiresProject = true,
		threadSafe = true,
		requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CouchAppDeployMojo extends CouchAppMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {

			debug();

			if (skip) {
				getLog().info("Skipping.");
				return;
			}

			initialize();

			final File sourceFolder = checkSource();
			if (sourceFolder == null) return;
			checkCouchAppResource(sourceFolder);

			final File targetFolder = checkTarget();

			final JsonObject couchapp = readFileAsJsonObject(new File(targetFolder.toString(), "couchapp.json"));

			if (!databaseExists()) {
				createDatabase();
			}

			final JsonObject original = getDesignDocument(couchapp.get("_id").getAsString());
			if (original != null) {
				couchapp.addProperty("_rev", getStringFromJson(original, "_rev"));
			}

			deployDesignDocument(couchapp);

		} catch (IOException e) {
			throw new MojoExecutionException("Something went wrong.", e);
		}
	}

	private void createDatabase() throws IOException {
		final CouchDbResponse response = getClient().createDatabase();
		if (response.getStatus() != 201) {
			throw new IOException("Unable to create database");
		}
	}

	private boolean databaseExists() throws IOException {
		final CouchDbResponse response = getClient().checkForDatabase();
		switch (response.getStatus()) {
		case 200:
			return true;
		case 404:
			return false;
		default:
			throw new IOException("An error occurred while checking for your database!");
		}
	}

	private void deployDesignDocument(final JsonObject designDocument) throws IOException {
		final CouchDbResponse response = getClient().updateDesignDocument(designDocument.get("_id").getAsString(), designDocument.toString());
		if (response.getStatus() != 201) {
			throw new IOException("Unable to save design document");
		}
	}

	private JsonObject getDesignDocument(String designDocumentId) throws IOException {
		final CouchDbResponse response = getClient().getDesignDocument(designDocumentId);
		switch (response.getStatus()) {
		case 200: return response.asJsonObject();
		case 404: return null;
		default: throw new IOException("Unable to retrieve design document!");
		}
	}

}
