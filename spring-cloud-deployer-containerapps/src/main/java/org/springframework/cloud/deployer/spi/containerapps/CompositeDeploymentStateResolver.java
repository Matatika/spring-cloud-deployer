package org.springframework.cloud.deployer.spi.containerapps;

import org.springframework.cloud.deployer.spi.app.DeploymentState;

import io.fabric8.kubernetes.api.model.ContainerStatus;

/**
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author David Turanski
 **/
class CompositeDeploymentStateResolver implements RunningPhaseDeploymentStateResolver {
	private final RunningPhaseDeploymentStateResolver[] delegates;

	CompositeDeploymentStateResolver(RunningPhaseDeploymentStateResolver... delegates) {
		this.delegates = delegates;
	}

	@Override
	public DeploymentState resolve(ContainerStatus containerStatus) {
		for (RunningPhaseDeploymentStateResolver resolver: delegates) {
			DeploymentState deploymentState = resolver.resolve(containerStatus);
			if (deploymentState != null) {
				return deploymentState;
			}
		}
		return null;
	}
}
