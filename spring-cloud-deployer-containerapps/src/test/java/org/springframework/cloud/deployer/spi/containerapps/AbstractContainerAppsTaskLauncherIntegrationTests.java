/*
 * Copyright 2021-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.spi.containerapps;

import static org.awaitility.Awaitility.await;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.test.AbstractTaskLauncherIntegrationJUnit5Tests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;

import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;
import com.azure.resourcemanager.appcontainers.fluent.models.JobExecutionInner;
import com.azure.resourcemanager.appcontainers.fluent.models.JobInner;
import com.azure.resourcemanager.appcontainers.fluent.models.ReplicaInner;
import com.azure.resourcemanager.appcontainers.fluent.models.RevisionInner;

/**
 * Abstract base class for integration tests for {@link ContainerAppsTaskLauncher}.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Bono
 */
abstract class AbstractContainerAppsTaskLauncherIntegrationTests extends AbstractTaskLauncherIntegrationJUnit5Tests {

	@Autowired
	protected TaskLauncher taskLauncher;

	@Autowired
	protected ContainerAppsApiClient client;

	@Autowired
	protected ContainerAppsDeployerProperties deployerProperties;

	@Override
	protected TaskLauncher provideTaskLauncher() {
		return taskLauncher;
	}

	@Override
	protected String randomName() {
		return "task-" + UUID.randomUUID().toString().substring(0, 18);
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:latest");
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(20, 5000);
	}

	@Test
	@Override
	public void testSimpleCancel() throws InterruptedException {
		super.testSimpleCancel();
	}

	protected void logTestInfo(TestInfo testInfo) {
		log.info("Testing {}...", testInfo.getTestMethod().map(Method::getName).orElse(testInfo.getDisplayName()));
	}

	protected ConditionFactory awaitWithPollAndTimeout(Timeout timeout) {
		return await().pollInterval(Duration.ofMillis(timeout.pause))
				.atMost(Duration.ofMillis((long) timeout.maxAttempts * (long) timeout.pause));
	}

	protected RevisionInner getRevisionForTask(String taskName) {
		Optional<RevisionInner> revision = client.getContainerAppsRevisions()
				.listRevisions(deployerProperties.getResourceGroup(), taskName)
				.stream()
				.filter(r -> r.active())
				.findFirst();
		if (!revision.isPresent())
			return null;
		return revision.get();
	}

	protected List<ReplicaInner> getReplicasForTask(String taskName, RevisionInner revision) {
		List<ReplicaInner> replicas = client.getContainerAppsRevisionReplicas()
				.listReplicas(deployerProperties.getResourceGroup(), taskName, revision.name())
				.value();
		return replicas;
	}

	protected List<ReplicaInner> getReplicasForTask(String taskName) {
		Optional<RevisionInner> revision = client.getContainerAppsRevisions()
				.listRevisions(deployerProperties.getResourceGroup(), taskName)
				.stream()
				.filter(r -> r.active())
				.findFirst();
		if (!revision.isPresent())
			return null;
		List<ReplicaInner> replicas = client.getContainerAppsRevisionReplicas()
				.listReplicas(deployerProperties.getResourceGroup(), taskName, revision.get().name())
				.value();
		return replicas;
	}

	protected List<JobInner> getJobsForTask(String taskName) {
		List<JobInner> jobs = client.getJobs()
				.listByResourceGroup(deployerProperties.getResourceGroup())
				.stream()
				.filter(job -> {
					String value = job.tags() != null ? job.tags().get("task-name") : null;
					return value != null && value.equals(taskName);
				})
				.collect(Collectors.toList());
		return jobs;
	}

	protected List<JobExecutionInner> getJobsExecutionsForTask(JobInner job) {
		List<JobExecutionInner> jobExecutions = client.getJobsExecutions()
				.list(deployerProperties.getResourceGroup(), job.name())
				.stream()
				.collect(Collectors.toList());
		return jobExecutions;
	}

}
