package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;

import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;

/**
 * Base class for TCP based probe creators
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @since 2.5
 */
abstract class TcpProbeCreator extends ProbeCreator {
    TcpProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties,
                    ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    abstract Integer getPort();

    protected abstract int getTimeout();

    protected Probe create() {
        return new ProbeBuilder()
                .withNewTcpSocket()
                .withNewPort(getPort())
                .endTcpSocket()
                .withInitialDelaySeconds(getInitialDelay())
                .withPeriodSeconds(getPeriod())
                .withFailureThreshold(getFailure())
                .withSuccessThreshold(getSuccess())
                .build();
    }
}
