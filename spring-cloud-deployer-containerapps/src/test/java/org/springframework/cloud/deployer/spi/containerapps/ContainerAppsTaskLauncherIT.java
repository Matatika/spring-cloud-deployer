package org.springframework.cloud.deployer.spi.containerapps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.containerapps.support.ContainerAppsStatusUtil;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.core.io.Resource;

import com.azure.resourcemanager.appcontainers.fluent.models.JobExecutionInner;
import com.azure.resourcemanager.appcontainers.fluent.models.JobInner;
import com.azure.resourcemanager.appcontainers.models.DefaultErrorResponseErrorException;

/**
 * Integration tests for {@link ContainerAppsTaskLauncher}.
 *
 * <p>NOTE: The tests do not call {@code TaskLauncher.destroy/cleanup} in a finally block but instead rely on the
 * {@link AbstractContainerAppsTaskLauncherIntegrationTests#cleanupLingeringApps() AfterEach method} to clean any stray apps.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Thomas Risberg
 * @author Chris Schaefer
 * @author Chris Bono
 * @author Glenn Renfro
 */
@SpringBootTest(classes = {ContainerAppsAutoConfiguration.class}, properties = {
		/*
		"spring.cloud.deployer.containerapps.resourceGroup=[your resource group]",
		"spring.cloud.deployer.containerapps.location=[the Azure region]",
		"spring.cloud.deployer.containerapps.environment=[the container app environment name]",
		"spring.cloud.deployer.containerapps.logAnalyticsWorkspaceId=[the workspace id of the log analytics workspace]",
		*/
})
@ExtendWith(OutputCaptureExtension.class)
public class ContainerAppsTaskLauncherIT extends AbstractContainerAppsTaskLauncherIntegrationTests {

	@Test
	void taskLaunchedWithLogs(TestInfo testInfo) {
		logTestInfo(testInfo);
		Map<String, String> appProperties = new HashMap<>();
		appProperties.put("killDelay", "3");
		appProperties.put("exitCode", "0");
		AppDefinition definition = new AppDefinition(randomName(), appProperties);
		Resource resource = testApplication();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, null);
		String taskName = request.getDefinition().getName();

		log.info("Launching {}...", taskName);
		String launchId = taskLauncher().launch(request);
		awaitWithPollAndTimeout(deploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.complete));

		log.info("Checking logs available {}...", taskName);
		awaitWithPollAndTimeout(deploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().getLog(launchId)).
						contains("Will kill this process in 3ms"));

		log.info("Cleaning up {}...", taskName);
		taskLauncher().cleanup(launchId);
		awaitWithPollAndTimeout(undeploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.unknown));
	}

	@Test
	void taskLaunchedWithDeploymentLabels(TestInfo testInfo) {
		logTestInfo(testInfo);
		launchTaskPodAndValidateCreatedPodWithCleanup(
				Collections.singletonMap("spring.cloud.deployer.containerapps.deploymentLabels", "label1:value1,label2:value2"),
				(job) -> {
					assertThat(job.name()).isNotEmpty();
					assertThat(job.tags()).isNotEmpty()
						.contains(entry("label1", "value1"), entry("label2", "value2"));
				},
				(jobExecutions) -> {
					assertThat(jobExecutions).hasSize(1);
					assertThat(jobExecutions)
						.singleElement()
						.satisfies((jobExecution) -> {
							assertThat(jobExecution.name()).isNotEmpty();
							assertThat(jobExecution.template().containers()).hasSize(1);
						});
				});
	}

	@Test
	void taskLaunchedWithContainerResourcesCpuMemory(TestInfo testInfo) {
		logTestInfo(testInfo);
		launchTaskPodAndValidateCreatedPodWithCleanup(
				Collections.emptyMap(),
				(job) -> {
					assertThat(job.name()).isNotEmpty();
				},
				(jobExecutions) -> {
					assertThat(jobExecutions).hasSize(1);
					assertThat(jobExecutions)
						.singleElement()
						.satisfies((jobExecution) -> {
							assertThat(jobExecution.name()).isNotEmpty();
							assertThat(jobExecution.template().containers())
									.singleElement()
									.extracting("resources")
									.hasFieldOrPropertyWithValue("cpu", 2.0)
									.hasFieldOrPropertyWithValue("memory", "4Gi");
						});
				});
	}

	@Test
	void tasksLaunchedWithAdditionalContainers(TestInfo testInfo) {
		/*
		 * NB - the spring properties binder does not support the Azure Container
		 * class as this uses a Fluent interface. e.g. withImage whereas the
		 * Spring Binder requires a POJO with get/set
		 */
		logTestInfo(testInfo);
		launchTaskPodAndValidateCreatedPodWithCleanup(
				Collections.singletonMap("spring.cloud.deployer.containerapps.additionalContainers",
						"[{name: 'test', image: 'busybox:latest', command: ['sh', '-c', 'echo hello']}]"),
				(job) -> {
					assertThat(job.name()).isNotEmpty();
				},
				(jobExecutions) -> {
					assertThat(jobExecutions).hasSize(1);
					assertThat(jobExecutions.get(0).template().containers())
						.filteredOn("name", "test").singleElement()
						.hasFieldOrPropertyWithValue("image", "busybox:latest")
						.hasFieldOrPropertyWithValue("command", Arrays.asList("sh", "-c", "echo hello"));
				});
	}

	private void launchTaskPodAndValidateCreatedPodWithCleanup(Map<String, String> deploymentProps, Consumer<JobInner> assertingJobConsumer, Consumer<? super List<? extends JobExecutionInner>> assertingJobExecutionsConsumer) {
		String taskName = randomName();
		AppDefinition definition = new AppDefinition(taskName, null);
		Resource resource = testApplication();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, deploymentProps);

		log.info("Launching {}...", taskName);
		String launchId = taskLauncher().launch(request);
		awaitWithPollAndTimeout(deploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.running));

		log.info("Checking task Job for {}...", taskName);
		List<JobInner> jobs = getJobsForTask(taskName);
		assertThat(jobs).hasSize(1);
		assertThat(jobs).singleElement().satisfies(assertingJobConsumer);
		JobInner job = jobs.get(0);

		log.info("Checking task Job Executions for {}...", taskName);
		List<JobExecutionInner> jobExecutions = getJobsExecutionsForTask(job);
		assertThat(jobExecutions).satisfies(assertingJobExecutionsConsumer);

		log.info("Destroying {}...", taskName);
		taskLauncher().destroy(taskName);
		awaitWithPollAndTimeout(undeploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.unknown));
	}

	@Test
	void cleanupDeletesTaskApp(TestInfo testInfo) {
		logTestInfo(testInfo);
		AppDefinition definition = new AppDefinition(randomName(), null);
		Resource resource = testApplication();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, null);
		String taskName = request.getDefinition().getName();

		log.info("Launching {}...", taskName);
		String launchId = taskLauncher().launch(request);
		awaitWithPollAndTimeout(deploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.running));

		List<JobInner> jobs = getJobsForTask(taskName);
		assertThat(jobs).hasSize(1);
		List<JobExecutionInner> jobExecutions = getJobsExecutionsForTask(jobs.get(0));
		assertThat(jobExecutions).hasSize(1);
		assertThat(ContainerAppsStatusUtil.isRunning(jobExecutions.get(0))).isTrue();

		log.info("Cleaning up {}...", taskName);
		taskLauncher().cleanup(launchId);
		awaitWithPollAndTimeout(undeploymentTimeout())
				.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.unknown));

		jobs = getJobsForTask(taskName);
		assertThat(jobs).isEmpty();
	}

	@Test
	void launchForNonExistentRepositoryThrowsException(TestInfo testInfo, CapturedOutput taskOutput) {
		logTestInfo(testInfo);
		// given empty app configuration, we don't expect any launch
		Map<String, String> appProperties = new HashMap<>();
		// given username for registry
		// given no passwordSecretRef
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("spring.cloud.deployer.containerapps.jobConfiguration", 
				"{ registries: [{ server: 'docker.io', username: 'foo',  }] }");
		AppDefinition definition = new AppDefinition(randomName(), appProperties);
		// given default test application
		Resource resource = new DockerResource("docker.io/busybox:latest");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, deploymentProperties);

		// when launch
		log.info("Launching {}...", request.getDefinition().getName());
		DefaultErrorResponseErrorException exception = assertThrows(DefaultErrorResponseErrorException.class, 
				() -> taskLauncher().launch(request));
		// expect a message indicating registries with username was set
		assertThat(exception.getMessage()).contains("PasswordSecretRef '' defined for registry server 'docker.io' not found.");

		// expect a message indicating registries with username was set
		assertThat(taskOutput.getAll()).contains("PasswordSecretRef '' defined for registry server 'docker.io' not found.");
	}

	/*
	 * This test functions correctly, but is flakey because azure is slow
	 */
	@Test
	@Disabled
	void launchWithRepository(TestInfo testInfo, CapturedOutput taskOutput) {
		logTestInfo(testInfo);
		// given empty app configuration, we don't expect any launch
		Map<String, String> appProperties = new HashMap<>();
		// given SystemAssigned identity for registry
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("spring.cloud.deployer.containerapps.identity.type", 
				"SystemAssigned");
		deploymentProperties.put("spring.cloud.deployer.containerapps.jobConfiguration", 
				"{ registries: [{ server: 'matatika.azurecr.io', identity: 'system' }] }");
		AppDefinition definition = new AppDefinition(randomName(), appProperties);
		// given default test application
		Resource resource = new DockerResource("matatika.azurecr.io/matatika/matatika-catalog-shelltask:latest-dev");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, deploymentProperties);
		// when launch
		log.info("Launching {}...", request.getDefinition().getName());
		String launchId = taskLauncher().launch(request);		
		// expect success status no exceptions
		awaitWithPollAndTimeout(deploymentTimeout())
			.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.complete));
		assertThat(taskOutput.getAll()).doesNotContain("error");
	}

	/*
	 * This test functions correctly, but needs a password defined in the environment to be enabled.
	 */
	@Test
	void launchWithRepositoryWithUsername(TestInfo testInfo) {
		logTestInfo(testInfo);
		// given a registry password is set
		String registryPassword = System.getenv("REGISTRY_PASSWORD");
		assertThat(registryPassword).as("REGISTRY_PASSWORD environment must be set").isNotEmpty();
		// given empty app configuration, we don't expect any launch
		Map<String, String> appProperties = new HashMap<>();
		// given SystemAssigned identity for registry
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("spring.cloud.deployer.containerapps.jobConfiguration", 
				"{"
				+ " secrets: [{ name: 'acr-password', value: '"+registryPassword+"'}]"
				+ " ,registries: [{ server: 'matatika.azurecr.io', username: 'matatika', passwordSecretRef: 'acr-password'}]"
				+ "}");
		AppDefinition definition = new AppDefinition(randomName(), appProperties);
		// given default test application
		Resource resource = new DockerResource("matatika.azurecr.io/matatika/matatika-catalog-shelltask:latest-dev");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, deploymentProperties);
		// when launch
		log.info("Launching {}...", request.getDefinition().getName());
		String launchId = taskLauncher().launch(request);		
		// expect failed status (this means it actually launched, but failed)
		awaitWithPollAndTimeout(deploymentTimeout())
			.untilAsserted(() -> assertThat(taskLauncher().status(launchId).getState()).isEqualTo(LaunchState.failed));
	}

	@Test
	void cleanupForNonExistentTaskThrowsException(TestInfo testInfo, CapturedOutput taskOutput) {
		logTestInfo(testInfo);
		taskLauncher().cleanup("foo");
		assertThat(taskOutput.getAll()).contains("Cannot delete Job for task \"foo\" (reason: Job does not exist)");
	}
}
