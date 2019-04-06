package org.apache.maven.plugin.couchapp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Upload a couchapp to CouchDB
 *
 */
@Mojo(
		name = "package",
		defaultPhase = LifecyclePhase.PACKAGE,
		requiresProject = true,
		threadSafe = true,
		requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CouchAppPackageMojo extends CouchAppMojo {

	private static final FileFilter onlyFolders = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};

	private static final FileFilter onlyJavascript = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.getName().endsWith(".js");
		}
	};

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

			final File targetFolder = checkTarget();

			final String designDocumentId = readFileAsString(new File(sourceFolder.toString(), "_id"));
			final String language = readFileAsString(new File(sourceFolder.toString(), "language"));
			final JsonObject couchapp = readFileAsJsonObject(new File(sourceFolder.toString(), "couchapp.json"));
			final JsonArray rewrites = readFileAsJsonArray(new File(sourceFolder.toString(), "rewrites.json"));
			final String readme = readFileAsString(new File(sourceFolder.toString(), "README.txt"));

			final JsonObject designDocument = createDesignDocument(
					designDocumentId,
					rewrites,
					language,
					readme,
					couchapp);
			if (debug) getLog().debug(prettyPrint(designDocument));
			saveDesignDocument(designDocument, targetFolder);

		} catch (IOException e) {
			throw new MojoExecutionException("Something went wrong.", e);
		}
	}

	private JsonObject createDesignDocument(
			final String documentId,
			final JsonArray rewrites,
			final String language,
			final String readme,
			final JsonObject couchapp) throws IOException {
		final JsonObject result = new JsonObject();

		result.addProperty("_id", documentId);

		final JsonArray manifest = new JsonArray();
		couchapp.add("manifest", manifest);

		manifest.add("couchapp.json");
		manifest.add("language");
		manifest.add("README.txt");
		manifest.add("rewrites.json");

		final JsonObject signatures = new JsonObject();
		couchapp.add("signatures", signatures);

		final JsonObject objects = new JsonObject();
		couchapp.add("objects", objects);

		result.add("rewrites", rewrites);
		result.addProperty("language", language);
		createViews(result, manifest);
		createLists(result, manifest);
		result.addProperty("README", readme);
		createShows(result, manifest);
		result.add("couchapp", couchapp);
		createAttachments(result, signatures);
		return result;
	}

	private void attachFiles(final File folder, final JsonObject list, final File root, final JsonObject signatures) throws IOException {
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				attachFiles(file, list, root, signatures);
			} else {
				final JsonObject attachment = new JsonObject();
				final String name = getRelativeFilename(file, root);
				list.add(name, attachment);
				final String type = Files.probeContentType(file.toPath());
				if (type == null) {
					attachment.addProperty("content_type", "application/octet-stream");
				} else {
					attachment.addProperty("content_type", type);
				}
				final byte[] data = FileUtils.readFileToByteArray(file);
				attachment.addProperty("data", base64encode(data));
				signatures.addProperty(name, md5(data));
			}
		}
	}

	private void createAttachments(final JsonObject designDocument, final JsonObject signatures) throws IOException {
		final File attachmentsFolder = new File(source, "_attachments");
		if (attachmentsFolder.isDirectory()) {
			final JsonObject attachments = new JsonObject();
			attachFiles(attachmentsFolder, attachments, attachmentsFolder, signatures);
			designDocument.add("_attachments", attachments);
		} else {
			getLog().warn("No attachments folder found!");
		}
	}

	private void createLists(final JsonObject designDocument, final JsonArray manifest) throws IOException {
		final File listsFolder = new File(source, "lists");
		if (listsFolder.isDirectory()) {
			final JsonObject lists = new JsonObject();
			manifest.add("lists/");
			for (File file : listsFolder.listFiles(onlyJavascript)) {
				lists.addProperty(getJavascriptName(file), readFileAsString(file));
				manifest.add("lists/" + getRelativeFilename(file, listsFolder));
			}
			designDocument.add("lists", lists);
		} else {
			getLog().warn("No lists folder found!");
		}
	}

	private void createShows(final JsonObject designDocument, final JsonArray manifest) throws IOException {
		final File showsFolder = new File(source, "shows");
		if (showsFolder.isDirectory()) {
			final JsonObject shows = new JsonObject();
			manifest.add("shows/");
			for (File file : showsFolder.listFiles(onlyJavascript)) {
				shows.addProperty(getJavascriptName(file), readFileAsString(file));
				manifest.add("shows/" + getRelativeFilename(file, showsFolder));
			}
			designDocument.add("shows", shows);
		} else {
			getLog().warn("No shows folder found!");
		}
	}

	private void createViews(final JsonObject designDocument, final JsonArray manifest) throws IOException {
		final File viewsFolder = new File(source, "views");
		if (viewsFolder.isDirectory()) {
			final JsonObject views = new JsonObject();
			manifest.add("views/");
			for (File folder : viewsFolder.listFiles(onlyFolders)) {
				final JsonObject view = new JsonObject();
				views.add(folder.getName(), view);
				manifest.add("views/" + getRelativeFilename(folder, viewsFolder) + "/");
				for (File file : folder.listFiles(onlyJavascript)) {
					view.addProperty(getJavascriptName(file), readFileAsString(file));
					manifest.add("views/" + getRelativeFilename(file, viewsFolder));
				}
			}
			designDocument.add("views", views);
		} else {
			getLog().warn("No views folder found!");
		}
	}

	private String getJavascriptName(final File file) {
		final String result = file.getName();
		return result.substring(0, result.length() - 3);
	}

	private String getRelativeFilename(final File file, final File root) throws IOException {
		if (file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
			return file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1).replace("\\", "/");
		}
		return null;
	}

	private void saveDesignDocument(final JsonObject designDocument, final File targetFolder) throws IOException {
		FileUtils.writeStringToFile(new File(targetFolder.toString(), "couchapp.json"), designDocument.toString(), StandardCharsets.UTF_8);
	}

}
