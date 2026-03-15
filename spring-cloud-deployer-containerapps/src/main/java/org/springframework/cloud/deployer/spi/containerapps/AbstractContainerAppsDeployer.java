/*
 * Copyright 2015-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties.Identity;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import com.azure.monitor.query.LogsQueryClient;
import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;
import com.azure.resourcemanager.appcontainers.fluent.models.ContainerAppInner;
import com.azure.resourcemanager.appcontainers.fluent.models.JobInner;
import com.azure.resourcemanager.appcontainers.fluent.models.ManagedEnvironmentInner;
import com.azure.resourcemanager.appcontainers.models.JobConfiguration;
import com.azure.resourcemanager.appcontainers.models.JobConfigurationManualTriggerConfig;
import com.azure.resourcemanager.appcontainers.models.ManagedServiceIdentity;
import com.azure.resourcemanager.appcontainers.models.ManagedServiceIdentityType;
import com.azure.resourcemanager.appcontainers.models.RegistryCredentials;
import com.azure.resourcemanager.appcontainers.models.Secret;
import com.azure.resourcemanager.appcontainers.models.TriggerType;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;

/**
 * Abstract base class for a deployer that targets Kubernetes.
 *
 * @author Florian Rosenberg
 * @author Thomas Risberg
 * @author Mark Fisher
 * @author Donovan Muller
 * @author David Turanski
 * @author Chris Schaefer
 * @author Enrique Medina Montenegro
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 */
public class AbstractContainerAppsDeployer {

	protected static final String SPRING_DEPLOYMENT_KEY = "spring-deployment-id";
	protected static final String SPRING_GROUP_KEY = "spring-group-id";
	protected static final String SPRING_APP_KEY = "spring-app-id";
	protected static final String SPRING_MARKER_KEY = "role";
	protected static final String SPRING_MARKER_VALUE = "spring-app";
	protected static final String APP_NAME_PROPERTY_KEY = AppDeployer.PREFIX + "appName";
	protected static final String APP_NAME_KEY = "spring-application-name";

	private static final String SERVER_PORT_KEY = "server.port";

	protected final Log logger = LogFactory.getLog(getClass().getName());

	protected ContainerFactory containerFactory;

	protected ContainerAppsApiClient client;

	protected LogsQueryClient logsQueryClient;

	protected ContainerAppsDeployerProperties properties;

	protected DeploymentPropertiesResolver deploymentPropertiesResolver;

	protected ManagedEnvironmentInner managedEnvironment;

	/**
	 * Create the RuntimeEnvironmentInfo.
	 *
	 * @param spiClass the SPI interface class
	 * @param implementationClass the SPI implementation class
	 * @return the Kubernetes runtime environment info
	 */
	protected RuntimeEnvironmentInfo createRuntimeEnvironmentInfo(Class<?> spiClass, Class<?> implementationClass) {
		return new RuntimeEnvironmentInfo.Builder()
				.spiClass(spiClass)
				.implementationName(implementationClass.getSimpleName())
				.implementationVersion(RuntimeVersionUtils.getVersion(implementationClass))
				.platformType("ContainerApps")
				.platformApiVersion(client.getApiVersion())
				.platformClientVersion(RuntimeVersionUtils.getVersion(client.getClass()))
				.platformHostVersion("unknown")
				.addPlatformSpecificInfo("endpoint", String.valueOf(client.getEndpoint()))
				.addPlatformSpecificInfo("subscriptionId", client.getSubscriptionId())
				.addPlatformSpecificInfo("resourceGroup", properties.getResourceGroup())
				.build();
	}

	/**
	 * Creates a map of labels for a given application ID.
	 *
	 * @param appId the application id
	 * @param request The {@link AppDeploymentRequest}
	 * @return the built id map of labels
	 */
	Map<String, String> createIdMap(String appId, AppDeploymentRequest request) {
		Map<String, String> map = new HashMap<>();
		map.put(SPRING_APP_KEY, appId);
		String groupId = request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY);
		if (groupId != null) {
			map.put(SPRING_GROUP_KEY, groupId);
		}
		map.put(SPRING_DEPLOYMENT_KEY, appId);

		// un-versioned app name provided by skipper
		String appName = request.getDeploymentProperties().get(APP_NAME_PROPERTY_KEY);

		if (StringUtils.hasText(appName)) {
			map.put(APP_NAME_KEY, appName);
		}

		return map;
	}

	protected AppStatus buildAppStatus(String id, PodList podList, ServiceList services) {
		AppStatus.Builder statusBuilder = AppStatus.of(id);
		Service service = null;
		if (podList != null && podList.getItems() != null) {
			for (Pod pod : podList.getItems()) {
				String deploymentKey = pod.getMetadata().getLabels().get(SPRING_DEPLOYMENT_KEY);
				for (Service svc : services.getItems()) {
					// handle case of when the version provided by skipper has been removed
					if(deploymentKey.startsWith(svc.getMetadata().getName())) {
						service = svc;
						break;
					}
				}
				//find the container with the correct env var
				for(Container container : pod.getSpec().getContainers()) {
					if(container.getEnv().stream().anyMatch(envVar -> "SPRING_CLOUD_APPLICATION_GUID".equals(envVar.getName()))) {
						//find container status for this container
						Optional<ContainerStatus> containerStatusOptional =
							pod.getStatus().getContainerStatuses()
							   .stream().filter(containerStatus -> container.getName().equals(containerStatus.getName()))
							   .findFirst();

						statusBuilder.with(new ContainerAppsAppInstanceStatus(pod, service, properties, containerStatusOptional.orElse(null)));

						break;
					}
				}
			}
		}
		return statusBuilder.build();
	}

	protected void logPossibleDownloadResourceMessage(Resource resource) {
		if (logger.isInfoEnabled()) {
			logger.info("Preparing to run a container from  " + resource
					+ ". This may take some time if the image must be downloaded from a remote container registry.");
		}
	}

	JobInner createJobInner(AppDeploymentRequest appDeploymentRequest) {
		String appId = createDeploymentId(appDeploymentRequest);
		Map<String, String>  deploymentProperties = appDeploymentRequest.getDeploymentProperties();

		JobInner job = new JobInner();
		job.withLocation(resolveLocation(deploymentProperties));
		job.withEnvironmentId(managedEnvironment.id());

		Identity identity = this.deploymentPropertiesResolver.getIdentity(deploymentProperties);
		if (identity != null) {
			job.withIdentity(new ManagedServiceIdentity().withType(ManagedServiceIdentityType.fromString(identity.getType().name())));
		}

		ContainerConfiguration containerConfiguration = new ContainerConfiguration(appId, appDeploymentRequest)
				.withHostNetwork(this.deploymentPropertiesResolver.getHostNetwork(deploymentProperties));
		job.withTemplate(containerFactory.createJobTemplate(containerConfiguration));

		List<Secret> secrets = new ArrayList<>();
		this.deploymentPropertiesResolver
				.getJobConfiguration(deploymentProperties)
				.getSecrets()
				.stream()
				.forEach(s -> {
					Secret secret = new Secret()
							.withName(s.getName());
					if (s.getValue() != null) {
						secret.withValue(s.getValue());
					}
					if (s.getIdentity() != null) {
						secret.withIdentity(s.getIdentity());
					}
					if (s.getKeyVaultUrl() != null) {
						secret.withKeyVaultUrl(s.getKeyVaultUrl());
					}
					secrets.add(secret);
				});
		
		List<RegistryCredentials> registries = new ArrayList<>();
		this.deploymentPropertiesResolver
				.getJobConfiguration(deploymentProperties)
				.getRegistries()
				.stream()
				.forEach(r -> {
					RegistryCredentials registry = new RegistryCredentials()
							.withServer(r.getServer());
					if (r.getIdentity() != null) {
						registry.withIdentity(r.getIdentity());
					}
					if (r.getUsername() != null) {
						registry.withUsername(r.getUsername());
					}
					if (r.getPasswordSecretRef() != null) {
						registry.withPasswordSecretRef(r.getPasswordSecretRef());
					}
					registries.add(registry);
				});
		job.withConfiguration(new JobConfiguration()
				.withTriggerType(TriggerType.MANUAL)
				.withManualTriggerConfig(new JobConfigurationManualTriggerConfig())
				.withReplicaTimeout(Integer.MAX_VALUE)
				.withSecrets(secrets)
				.withRegistries(registries));
		return job;
	}
	
	/**
	 * Create ContainerAppInner for the given {@link AppDeploymentRequest}

	 * @param appDeploymentRequest the app deployment request to use to create the ContainerAppInner
	 * @return the ContainerAppInner
	 */
	ContainerAppInner createContainerAppInner(AppDeploymentRequest appDeploymentRequest) {
		String appId = createDeploymentId(appDeploymentRequest);
		Map<String, String>  deploymentProperties = appDeploymentRequest.getDeploymentProperties();

		ContainerAppInner containerApp = new ContainerAppInner();

		Identity identity = this.deploymentPropertiesResolver.getIdentity(deploymentProperties);
		if (identity != null) {
			containerApp.withIdentity(new ManagedServiceIdentity());
			// imagePullSecret;
		}

//		List<String> imagePullSecrets = this.deploymentPropertiesResolver.getImagePullSecrets(deploymentProperties);
//		if (imagePullSecrets != null) {
//			imagePullSecrets.forEach(imgPullsecret -> podSpec.addNewImagePullSecret(imgPullsecret));
//		}

		boolean hostNetwork = this.deploymentPropertiesResolver.getHostNetwork(deploymentProperties);
		ContainerConfiguration containerConfiguration = new ContainerConfiguration(appId, appDeploymentRequest)
				.withHostNetwork(hostNetwork);

// ports not supported
//		if (ContainerAppsAppDeployer.class.isAssignableFrom(this.getClass())) {
//			containerConfiguration.withExternalPort(getExternalPort(appDeploymentRequest));
//		}
		containerApp.withLocation(resolveLocation(deploymentProperties));

		String environment = this.deploymentPropertiesResolver.getEnvironment(deploymentProperties);;
		containerApp.withEnvironmentId(environment);

		containerApp.withTemplate(containerFactory.createAppTemplate(containerConfiguration));

		/*
		 * 
		// add memory and cpu resource limits
		containerApp.withWorkloadProfileName(workloadprofile??);
		ResourceRequirements req = new ResourceRequirements();
		req.setLimits(this.deploymentPropertiesResolver.deduceResourceLimits(deploymentProperties));
		req.setRequests(this.deploymentPropertiesResolver.deduceResourceRequests(deploymentProperties));
		container.setResources(req);
		*/

		/*
		 * 
//		ImagePullPolicy pullPolicy = this.deploymentPropertiesResolver.deduceImagePullPolicy(deploymentProperties);
//		container.setImagePullPolicy(pullPolicy.name());
		 */

		/*
		 * 
		ContainerAppsDeployerProperties.Lifecycle lifecycle =
				this.deploymentPropertiesResolver.getLifeCycle(deploymentProperties);

		Lifecycle f8Lifecycle = new Lifecycle();
		if (lifecycle.getPostStart() != null) {
			f8Lifecycle.setPostStart(new LifecycleHandlerBuilder()
					.withNewExec()
					.addAllToCommand(lifecycle.getPostStart().getExec().getCommand()).and().build());
		}
		if (lifecycle.getPreStop() != null) {
			f8Lifecycle.setPreStop(new LifecycleHandlerBuilder()
					.withNewExec()
					.addAllToCommand(lifecycle.getPreStop().getExec().getCommand()).and().build());
		}

		if (f8Lifecycle.getPostStart() != null || f8Lifecycle.getPreStop() != null) {
			container.setLifecycle(f8Lifecycle);
		}

		Long termGracePeriod = this.deploymentPropertiesResolver.determineTerminationGracePeriodSeconds(deploymentProperties);
		if (termGracePeriod != null) {
			podSpec.withTerminationGracePeriodSeconds(termGracePeriod);
		}

		podSpec.withTolerations(this.deploymentPropertiesResolver.getTolerations(deploymentProperties));
		 */

		/*
		 * 
		// only add volumes with corresponding volume mounts
		podSpec.withVolumes(this.deploymentPropertiesResolver.getVolumes(deploymentProperties).stream()
				.filter(volume -> container.getVolumeMounts().stream()
						.anyMatch(volumeMount -> volumeMount.getName().equals(volume.getName())))
				.collect(Collectors.toList()));
		 */

		/*
		 * 
		if (hostNetwork) {
			podSpec.withHostNetwork(true);
		}
		 */

		/*
		 *
		SecurityContext containerSecurityContext = this.deploymentPropertiesResolver.getContainerSecurityContext(deploymentProperties);
		if (containerSecurityContext != null) {
			container.setSecurityContext(containerSecurityContext);
		}
		podSpec.addToContainers(container);
		 */

		/*
		 * 
		podSpec.withRestartPolicy(this.deploymentPropertiesResolver.getRestartPolicy(deploymentProperties).name());
		 */

		/*
		 * 
		String deploymentServiceAccountName = this.deploymentPropertiesResolver.getDeploymentServiceAccountName(deploymentProperties);
		if (deploymentServiceAccountName != null) {
			podSpec.withServiceAccountName(deploymentServiceAccountName);
		}
		 */

		/*
		 * 
		PodSecurityContext podSecurityContext = this.deploymentPropertiesResolver.getPodSecurityContext(deploymentProperties);
		if (podSecurityContext != null) {
			podSpec.withSecurityContext(podSecurityContext);
		}
		 */

		/*
		 * 
		Container initContainer = this.deploymentPropertiesResolver.getInitContainer(deploymentProperties);
		if (initContainer != null) {
			if (initContainer.getSecurityContext() == null && containerSecurityContext != null) {
				initContainer.setSecurityContext(containerSecurityContext);
			}
			podSpec.addToInitContainers(initContainer);
		}
		 */

		/*
		 * 
		Boolean shareProcessNamespace = this.deploymentPropertiesResolver.getShareProcessNamespace(deploymentProperties);
		if (shareProcessNamespace != null) {
			podSpec.withShareProcessNamespace(shareProcessNamespace);
		}
		 */

		/*
		 * 
		String priorityClassName = this.deploymentPropertiesResolver.getPriorityClassName(deploymentProperties);
		if (StringUtils.hasText(priorityClassName)) {
			podSpec.withPriorityClassName(priorityClassName);
		}
		 */

		/*
		 * 
		List<Container> additionalContainers = this.deploymentPropertiesResolver.getAdditionalContainers(deploymentProperties);
		if (containerSecurityContext != null && !CollectionUtils.isEmpty(additionalContainers)) {
			additionalContainers.stream().filter((c) -> c.getSecurityContext() == null)
					.forEach((c) -> c.setSecurityContext(containerSecurityContext));
		}
		podSpec.addAllToContainers(additionalContainers);
		return podSpec.build();
		 */
		return containerApp;
	}

	int getExternalPort(final AppDeploymentRequest request) {
		int externalPort = 8080;
		Map<String, String> parameters = request.getDefinition().getProperties();
		if (parameters.containsKey(SERVER_PORT_KEY)) {
			externalPort = Integer.valueOf(parameters.get(SERVER_PORT_KEY));
		}

		return externalPort;
	}

	String createDeploymentId(AppDeploymentRequest request) {
		String groupId = request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY);
		String deploymentId;
		if (groupId == null) {
			deploymentId = String.format("%s", request.getDefinition().getName());
		}
		else {
			deploymentId = String.format("%s-%s", groupId, request.getDefinition().getName());
		}
		/* Container Apps (w/ Kubernetes under the covers) does not allow '.' in the name 
		 * and does not allow uppercase in the name
		 */
		return deploymentId.replace('.', '-').toLowerCase();
	}

	private String resolveLocation(Map<String, String> deploymentProperties) {
		final String location = deploymentPropertiesResolver.getLocation(deploymentProperties);
		return location != null ? location : managedEnvironment.location();
	}

}
