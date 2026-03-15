package org.springframework.cloud.deployer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsClientFactory;
import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;
import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a ContainerApps installation is available.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 */
public class KubernetesTestSupport extends AbstractExternalResourceTestSupport<ContainerAppsClient> {
	private ConfigurableApplicationContext context;

	public KubernetesTestSupport() {
		super("CONTAINERAPPS");
	}

	@Override
	public void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	public void obtainResource() throws Exception {
		context = new SpringApplicationBuilder(Config.class).web(WebApplicationType.NONE).run();
		resource = context.getBean(ContainerAppsClient.class);
		resource.list();
	}

	@Configuration
	@EnableAutoConfiguration
	public static class Config {
		private ContainerAppsDeployerProperties properties = new ContainerAppsDeployerProperties();

		@Bean
		public ContainerAppsApiClient containerAppsClient() {
			return ContainerAppsClientFactory.getContainerAppsApiClient(this.properties);
		}
	}
}
