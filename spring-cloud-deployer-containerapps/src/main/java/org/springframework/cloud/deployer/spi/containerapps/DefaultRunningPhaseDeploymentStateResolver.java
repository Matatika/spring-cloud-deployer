package org.springframework.cloud.deployer.spi.containerapps;

import org.springframework.cloud.deployer.spi.app.DeploymentState;

/**
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author David Turanski
 **/
public class DefaultRunningPhaseDeploymentStateResolver extends CompositeDeploymentStateResolver {

	public DefaultRunningPhaseDeploymentStateResolver(ContainerAppsDeployerProperties properties) {
		super(
			new PredicateRunningPhaseDeploymentStateResolver.ContainerReady(properties),
			new PredicateRunningPhaseDeploymentStateResolver.ContainerCrashed(properties),
			new PredicateRunningPhaseDeploymentStateResolver.RestartsDueToTheSameError(properties),
			new PredicateRunningPhaseDeploymentStateResolver.CrashLoopBackOffRestarts(properties),
			new PredicateRunningPhaseDeploymentStateResolver.ContainerTerminated(properties),
			//default
			containerStatus -> DeploymentState.deploying);
	}
}
