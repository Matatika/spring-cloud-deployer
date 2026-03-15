package org.springframework.cloud.deployer.spi.containerapps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties.Identity;
import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties.JobConfiguration;


/**
 * Test the {@link DeploymentPropertiesResolver} class
 * @author Aaron Phethean
 */
public class DeploymentPropertiesResolverTests {

	@Test
	public void testIdentityType() {
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		Identity identity = deploymentPropertiesResolver.getIdentity(properties);
		assertThat(identity).isNull();
		properties.put(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX 
				+ ".identity.type", IdentityType.SystemAssigned.name());
		identity = deploymentPropertiesResolver.getIdentity(properties);
		assertThat(identity.getType()).isEqualTo(IdentityType.SystemAssigned);
	}

	@Test
	public void testSecrets() {
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		JobConfiguration jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getSecrets()).hasSize(0);

		properties.put(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX 
		+ ".jobConfiguration", "{ secrets: ["
				+ "  {"
				+ "    name: 'foo',"
				+ "    identity: 'system'"
				+ "  }]"
				+ "}");

		jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getSecrets()).hasSize(1);
		assertThat(jobConfiguration.getSecrets().get(0).getName())
			.isEqualTo("foo");
		assertThat(jobConfiguration.getSecrets().get(0).getIdentity())
			.isEqualTo("system");
	}

	/*
	 * json structured secrets fail to bind with the standard spring Binder
	 * Assert that setting a structured secrets definition via string resolves correctly.
	 */
	@Test
	public void testSecretsStringDefinition() {
		// given no properties
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		JobConfiguration jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getSecrets()).hasSize(0);

		deployerProperties.setSecrets("["
				+ "  {"
				+ "    name: 'foo',"
				+ "    identity: 'system'"
				+ "  }"
				+ "]");

		jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getSecrets()).hasSize(1);
		assertThat(jobConfiguration.getSecrets().get(0).getName())
			.isEqualTo("foo");
		assertThat(jobConfiguration.getSecrets().get(0).getIdentity())
			.isEqualTo("system");
	}

	/*
	 * json structured registries fail to bind with the standard spring Binder
	 * Assert that setting a structured registries definition via string resolves correctly.
	 */
	@Test
	public void testResitriesStringDefinition() {
		// given no properties
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		JobConfiguration jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getRegistries()).hasSize(0);

		deployerProperties.setRegistries("["
				+ "  {"
				+ "    server: 'matatika.azurecr.io',"
				+ "    identity: 'system'"
				+ "  }"
				+ "]");

		jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getRegistries()).hasSize(1);
		assertThat(jobConfiguration.getRegistries().get(0).getServer())
			.isEqualTo("matatika.azurecr.io");
		assertThat(jobConfiguration.getRegistries().get(0).getIdentity())
			.isEqualTo("system");
	}

	@Test
	public void testRegistries() {
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		JobConfiguration jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getRegistries()).hasSize(0);

		properties.put(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX 
		+ ".jobConfiguration", "{ registries: ["
				+ "  {"
				+ "    server: 'matatika.azurecr.io',"
				+ "    identity: 'system'"
				+ "  }]"
				+ "}");

		jobConfiguration = deploymentPropertiesResolver.getJobConfiguration(properties);
		assertThat(jobConfiguration.getRegistries()).hasSize(1);
		assertThat(jobConfiguration.getRegistries().get(0).getServer())
			.isEqualTo("matatika.azurecr.io");
		assertThat(jobConfiguration.getRegistries().get(0).getIdentity())
			.isEqualTo("system");
	}

	@Test
	public void testRestartPolicy() {
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		RestartPolicy restartPolicy = deploymentPropertiesResolver.getRestartPolicy(properties);
		assertThat(restartPolicy).isEqualTo(RestartPolicy.Always);
		properties.put(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX 
				+ ".restartPolicy", RestartPolicy.Never.name());
		restartPolicy = deploymentPropertiesResolver.getRestartPolicy(properties);
		assertThat(restartPolicy).isEqualTo(RestartPolicy.Never);
		properties.clear();
		properties.put(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX 
				+ ".restart-policy", RestartPolicy.Never.name());
		restartPolicy = deploymentPropertiesResolver.getRestartPolicy(properties);
		assertThat(restartPolicy).isEqualTo(RestartPolicy.Never);
	}

	@Test
	public void testTaskServiceAccountName() {
		ContainerAppsDeployerProperties deployerProperties = new ContainerAppsDeployerProperties();
		DeploymentPropertiesResolver deploymentPropertiesResolver = new DeploymentPropertiesResolver(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, deployerProperties);
		Map<String, String> properties = new HashMap<>();
		String taskServiceAccountName = deploymentPropertiesResolver.getTaskServiceAccountName(properties);
		assertThat(taskServiceAccountName).isEqualTo("default");
		properties.put(ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX 
				+ ".task-service-account-name", "FOO");
		taskServiceAccountName = deploymentPropertiesResolver.getTaskServiceAccountName(properties);
		assertThat(taskServiceAccountName).isEqualTo("FOO");
	}

}
