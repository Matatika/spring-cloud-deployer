package org.springframework.cloud.deployer.spi.containerapps;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;

import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;

public class ContainerAppsAppDeployer implements AppDeployer {

	public ContainerAppsAppDeployer(ContainerAppsDeployerProperties deployerProperties,
			ContainerAppsApiClient containerAppsClient, ContainerFactory containerFactory) {
//		throw new RuntimeException("Not implemented");
	}

	@Override
	public String deploy(AppDeploymentRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void undeploy(String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public AppStatus status(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

//	private TaskStatus buildAppStatus(String deploymentId) {
//		RevisionInner revision = getAppActiveRevisionByDeploymentId(deploymentId);
//		if (revision == null) {
//			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
//		}
//
//		/*
//		 * https://learn.microsoft.com/en-us/azure/container-apps/revisions#provisioning-status
//		 */
//		RevisionProvisioningState provisioningState = revision.provisioningState();
//		if (provisioningState == null) {
//			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
//		}
//		switch (provisioningState.toString()) {
//		case "Provisioning":
//			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
//		case "Failed":
//			return new TaskStatus(deploymentId, LaunchState.failed, new HashMap<>());
//		}
//
//		/*
//		 * https://learn.microsoft.com/en-us/azure/container-apps/revisions#running-status
//		 */
//		RevisionRunningState runningState = revision.runningState();
//		if (runningState == null) {
//			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
//		}
//		switch (runningState.toString()) {
//		case "Provisioning":
//			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
//		case "Activating":
//			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
//		case "Failed":
//			return new TaskStatus(deploymentId, LaunchState.failed, new HashMap<>());
//		case "Degraded":
//			return new TaskStatus(deploymentId, LaunchState.failed, new HashMap<>());
//		case "Scale to 0":
//			return new TaskStatus(deploymentId, LaunchState.complete, new HashMap<>());
//		case "Deprovisioning":
//			return new TaskStatus(deploymentId, LaunchState.complete, new HashMap<>());
//		default:
//			return new TaskStatus(deploymentId, LaunchState.running, new HashMap<>());
//		}
//	}

//	/*
//	 * deploymentId is our internal name we generated for this Container App
//	 */
//	private RevisionInner getAppActiveRevisionByDeploymentId(String deploymentId) {
//		List<String> ids = getIdsForTasks(Optional.of(deploymentId), false);
//		Optional<String> containerAppId = ids.stream().findFirst();
//		if (!containerAppId.isPresent()) {
//			logger.debug(String.format("Cannot get active revision for task \"%s\" (reason: App does not exist)", deploymentId));
//			return null;
//		}
//		Optional<RevisionInner> revision = client.getContainerAppsRevisions()
//				.listRevisions(properties.getResourceGroup(), deploymentId).stream().filter(r -> r.active())
//				.findFirst();
//		if (!revision.isPresent()) {
//			logger.debug(String.format("No active revision for task \"%s\"", deploymentId));
//			return null;
//		}
//		return revision.get();
//	}

}
