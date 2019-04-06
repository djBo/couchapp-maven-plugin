package org.apache.maven.plugin.couchapp;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class CouchAppMojoTest {

	@Test
	public void testMojo() throws MojoExecutionException, MojoFailureException {
		final CouchAppMojo packager = new CouchAppPackageMojo();
		packager.setSource("src/test/resources");
		packager.setTarget("target/generated-test-resources");
		packager.execute();
	}

}
