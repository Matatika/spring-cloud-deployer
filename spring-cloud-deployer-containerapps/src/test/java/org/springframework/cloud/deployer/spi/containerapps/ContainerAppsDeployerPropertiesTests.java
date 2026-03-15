package org.springframework.cloud.deployer.spi.containerapps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertNotNull(containerAppsDeployerProperties.getImagePullPolicy(), "Image pull policy should not be null");
		assertEquals(ImagePullPolicy.IfNotPresent,
				containerAppsDeployerProperties.getImagePullPolicy(),
				"Invalid default image pull policy");
	}

	@Test
	public void testImagePullPolicyCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setImagePullPolicy(ImagePullPolicy.Never);
		assertNotNull(containerAppsDeployerProperties.getImagePullPolicy(), "Image pull policy should not be null");
		assertEquals(ImagePullPolicy.Never,
				containerAppsDeployerProperties.getImagePullPolicy(),
				"Unexpected image pull policy");
	}

	@Test
	public void testRestartPolicyDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull(containerAppsDeployerProperties.getRestartPolicy(), "Restart policy should not be null");
		assertEquals(RestartPolicy.Always,
				containerAppsDeployerProperties.getRestartPolicy(),
				"Invalid default restart policy");
	}

	@Test
	public void testRestartPolicyCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setRestartPolicy(RestartPolicy.OnFailure);
		assertNotNull(containerAppsDeployerProperties.getRestartPolicy(), "Restart policy should not be null");
		assertEquals(RestartPolicy.OnFailure,
				containerAppsDeployerProperties.getRestartPolicy(),
				"Unexpected restart policy");
	}

	@Test
	public void testEntryPointStyleDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull(containerAppsDeployerProperties.getEntryPointStyle(), "Entry point style should not be null");
		assertEquals(EntryPointStyle.exec,
				containerAppsDeployerProperties.getEntryPointStyle(),
				"Invalid default entry point style");
	}

	@Test
	public void testEntryPointStyleCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setEntryPointStyle(EntryPointStyle.shell);
		assertNotNull(containerAppsDeployerProperties.getEntryPointStyle(), "Entry point style should not be null");
		assertEquals(EntryPointStyle.shell,
				containerAppsDeployerProperties.getEntryPointStyle(),
				"Unexpected entry point stype");
	}

	@Test
	public void testNamespaceDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNull(containerAppsDeployerProperties.getResourceGroup());
		containerAppsDeployerProperties.setResourceGroup("default");

		assertTrue(StringUtils.hasText(containerAppsDeployerProperties.getResourceGroup()),
				"Resource Group should not be empty or null");
		assertEquals("default", containerAppsDeployerProperties.getResourceGroup(), "Invalid default namespace");
	}

	@Test
	public void testNamespaceCanBeCustomized() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setResourceGroup("myns");
		assertTrue(StringUtils.hasText(containerAppsDeployerProperties.getResourceGroup()),
				"Namespace should not be empty or null");
		assertEquals("myns", containerAppsDeployerProperties.getResourceGroup(), "Unexpected namespace");
	}

	@Test
	public void testImagePullSecretDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNull(containerAppsDeployerProperties.getImagePullSecret(), "No default image pull secret should be set");
	}

	@Test
	public void testImagePullSecretCanBeCustomized() {
		String secret = "mysecret";
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setImagePullSecret(secret);
		assertNotNull(containerAppsDeployerProperties.getImagePullSecret(), "Image pull secret should not be null");
		assertEquals(secret, containerAppsDeployerProperties.getImagePullSecret(), "Unexpected image pull secret");
	}

	@Test
	public void testEnvironmentVariablesDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertEquals(0,	containerAppsDeployerProperties.getEnvironmentVariables().length,
				"No default environment variables should be set");
	}

	@Test
	public void testEnvironmentVariablesCanBeCustomized() {
		String[] envVars = new String[] { "var1=val1", "var2=val2" };
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setEnvironmentVariables(envVars);
		assertNotNull(containerAppsDeployerProperties.getEnvironmentVariables(),
				"Environment variables should not be null");
		assertEquals(2,	containerAppsDeployerProperties.getEnvironmentVariables().length,
				"Unexpected number of environment variables");
	}

	@Test
	public void testTaskServiceAccountNameDefault() {
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		assertNotNull(containerAppsDeployerProperties.getTaskServiceAccountName(),
				"Task service account name should not be null");
		assertEquals(ContainerAppsDeployerProperties.DEFAULT_TASK_SERVICE_ACCOUNT_NAME,
				containerAppsDeployerProperties.getTaskServiceAccountName(),
				"Unexpected default task service account name");
	}

	@Test
	public void testTaskServiceAccountNameCanBeCustomized() {
		String taskServiceAccountName = "mysa";
		ContainerAppsDeployerProperties containerAppsDeployerProperties = new ContainerAppsDeployerProperties();
		containerAppsDeployerProperties.setTaskServiceAccountName(taskServiceAccountName);
		assertNotNull(containerAppsDeployerProperties.getTaskServiceAccountName(),
				"Task service account name should not be null");
		assertEquals(taskServiceAccountName,
				containerAppsDeployerProperties.getTaskServiceAccountName(),
				"Unexpected task service account name");
	}
}
