package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;

import io.fabric8.kubernetes.api.model.ExecActionBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;

/**
 * Base class for command based probe creators
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Corneil du Plessis
 * @since 2.5
 */
abstract class CommandProbeCreator extends ProbeCreator {
    CommandProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties,
                        ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    abstract String[] getCommand();

    protected Probe create() {
        ExecActionBuilder execActionBuilder = new ExecActionBuilder()
                .withCommand(getCommand());

        return new ProbeBuilder()
                .withExec(execActionBuilder.build())
                .withInitialDelaySeconds(getInitialDelay())
                .withPeriodSeconds(getPeriod())
                .withSuccessThreshold(getSuccess())
                .withFailureThreshold(getFailure())
                .build();
    }
}
