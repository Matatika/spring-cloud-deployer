package org.springframework.cloud.deployer.spi.containerapps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestTemplate;

import com.azure.monitor.query.LogsQueryClient;
import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;

/**
 * Spring Bean configuration for the {@link ContainerAppsAppDeployer}.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Florian Rosenberg
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 * @author Chris Schaefer
 */
@Configuration
@EnableConfigurationProperties({ContainerAppsDeployerProperties.class, ContainerAppsTaskLauncherProperties.class})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ContainerAppsAutoConfiguration {
	
	@Autowired
	private ContainerAppsDeployerProperties deployerProperties;

	@Autowired
	private ContainerAppsTaskLauncherProperties taskLauncherProperties;

	@Bean
	@ConditionalOnMissingBean(AppDeployer.class)
	public AppDeployer appDeployer(ContainerAppsApiClient containerAppsClient,
	                               ContainerFactory containerFactory) {
		return new ContainerAppsAppDeployer(deployerProperties, containerAppsClient, containerFactory);
	}

	@Bean
	@ConditionalOnMissingBean(TaskLauncher.class)
	public TaskLauncher taskDeployer(ContainerAppsApiClient containerAppsClient,
			LogsQueryClient logsQueryClient,
			ContainerFactory containerFactory) {
		return new ContainerAppsTaskLauncher(deployerProperties, taskLauncherProperties, containerAppsClient, logsQueryClient, containerFactory);
	}

	@Bean
	@ConditionalOnMissingBean(ContainerAppsApiClient.class)
	public ContainerAppsApiClient containerAppsApiClient() {
		return ContainerAppsClientFactory.getContainerAppsApiClient(this.deployerProperties);
	}

	@Bean
	@ConditionalOnMissingBean(LogsQueryClient.class)
	public LogsQueryClient logsQueryClient() {
		return ContainerAppsClientFactory.getLogsQueryClient(this.deployerProperties);
	}

	@Bean
	public ContainerFactory containerFactory() {
		return new DefaultContainerFactory(deployerProperties);
	}

//	@Bean
//	@ConditionalOnMissingBean(ActuatorOperations.class)
//	ActuatorOperations actuatorOperations(RestTemplate actuatorRestTemplate, AppDeployer appDeployer,
//			ContainerAppsDeployerProperties properties) {
//	}

	@Bean
	@ConditionalOnMissingBean
	RestTemplate actuatorRestTemplate() {
		//TODO: Configure security
		return new RestTemplate();
	}

}
