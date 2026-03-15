package org.springframework.cloud.deployer.spi.containerapps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link ContainerAppsDeployerProperties}.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Glenn Renfro
 */
public class ContainerAppsDeployerPropertiesTests {

	@Test
	public void testImagePullPolicyDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull("Image pull policy should not be null", containerAppsDeployerProperties.getImagePullPolicy());
		assertEquals("Invalid default image pull policy", ImagePullPolicy.IfNotPresent,
				containerAppsDeployerProperties.getImagePullPolicy());
	}

	@Test
	public void testImagePullPolicyCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setImagePullPolicy(ImagePullPolicy.Never);
		assertNotNull("Image pull policy should not be null", containerAppsDeployerProperties.getImagePullPolicy());
		assertEquals("Unexpected image pull policy", ImagePullPolicy.Never,
				containerAppsDeployerProperties.getImagePullPolicy());
	}

	@Test
	public void testRestartPolicyDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull("Restart policy should not be null", containerAppsDeployerProperties.getRestartPolicy());
		assertEquals("Invalid default restart policy", RestartPolicy.Always,
				containerAppsDeployerProperties.getRestartPolicy());
	}

	@Test
	public void testRestartPolicyCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setRestartPolicy(RestartPolicy.OnFailure);
		assertNotNull("Restart policy should not be null", containerAppsDeployerProperties.getRestartPolicy());
		assertEquals("Unexpected restart policy", RestartPolicy.OnFailure,
				containerAppsDeployerProperties.getRestartPolicy());
	}

	@Test
	public void testEntryPointStyleDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull("Entry point style should not be null", containerAppsDeployerProperties.getEntryPointStyle());
		assertEquals("Invalid default entry point style", EntryPointStyle.exec,
				containerAppsDeployerProperties.getEntryPointStyle());
	}

	@Test
	public void testEntryPointStyleCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setEntryPointStyle(EntryPointStyle.shell);
		assertNotNull("Entry point style should not be null", containerAppsDeployerProperties.getEntryPointStyle());
		assertEquals("Unexpected entry point stype", EntryPointStyle.shell,
				containerAppsDeployerProperties.getEntryPointStyle());
	}

	@Test
	public void testNamespaceDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNull(containerAppsDeployerProperties.getResourceGroup());
		containerAppsDeployerProperties.setResourceGroup("default");

		assertTrue("Resource Group should not be empty or null",
				StringUtils.hasText(containerAppsDeployerProperties.getResourceGroup()));
		assertEquals("Invalid default namespace", "default", containerAppsDeployerProperties.getResourceGroup());
	}

	@Test
	public void testNamespaceCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setResourceGroup("myns");
		assertTrue("Namespace should not be empty or null",
				StringUtils.hasText(containerAppsDeployerProperties.getResourceGroup()));
		assertEquals("Unexpected namespace", "myns", containerAppsDeployerProperties.getResourceGroup());
	}

	@Test
	public void testImagePullSecretDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNull("No default image pull secret should be set", containerAppsDeployerProperties.getImagePullSecret());
	}

	@Test
	public void testImagePullSecretCanBeCustomized() {
		String secret = "mysecret";
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setImagePullSecret(secret);
		assertNotNull("Image pull secret should not be null", containerAppsDeployerProperties.getImagePullSecret());
		assertEquals("Unexpected image pull secret", secret, containerAppsDeployerProperties.getImagePullSecret());
	}

	@Test
	public void testEnvironmentVariablesDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertEquals("No default environment variables should be set", 0,
				containerAppsDeployerProperties.getEnvironmentVariables().length);
	}

	@Test
	public void testEnvironmentVariablesCanBeCustomized() {
		String[] envVars = new String[] { "var1=val1", "var2=val2" };
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setEnvironmentVariables(envVars);
		assertNotNull("Environment variables should not be null",
				containerAppsDeployerProperties.getEnvironmentVariables());
		assertEquals("Unexpected number of environment variables", 2,
				containerAppsDeployerProperties.getEnvironmentVariables().length);
	}

	@Test
	public void testTaskServiceAccountNameDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull("Task service account name should not be null",
				containerAppsDeployerProperties.getTaskServiceAccountName());
		assertEquals("Unexpected default task service account name",
				ContainerAppsDeployerProperties.DEFAULT_TASK_SERVICE_ACCOUNT_NAME,
				containerAppsDeployerProperties.getTaskServiceAccountName());
	}

	@Test
	public void testTaskServiceAccountNameCanBeCustomized() {
		String taskServiceAccountName = "mysa";
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setTaskServiceAccountName(taskServiceAccountName);
		assertNotNull("Task service account name should not be null",
				containerAppsDeployerProperties.getTaskServiceAccountName());
		assertEquals("Unexpected task service account name", taskServiceAccountName,
				containerAppsDeployerProperties.getTaskServiceAccountName());
	}
}
