package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;

import io.fabric8.kubernetes.api.model.Probe;

/**
 * Creates health check probes
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @since 2.5
 */
public class ProbeCreatorFactory {
    public static Probe createStartupProbe(ContainerConfiguration containerConfiguration,
            ContainerAppsDeployerProperties containerAppsDeployerProperties, 
            ProbeType probeType) {
        switch (probeType) {
            case HTTP:
                return new StartupHttpProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            case TCP:
                return new StartupTcpProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            case COMMAND:
                return new StartupCommandProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            default:
                throw new IllegalArgumentException("Unknown startup probe type: " + probeType);
        }
    }

    public static Probe createReadinessProbe(ContainerConfiguration containerConfiguration,
    		ContainerAppsDeployerProperties containerAppsDeployerProperties, 
    		ProbeType probeType) {
        switch (probeType) {
            case HTTP:
                return new ReadinessHttpProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            case TCP:
                return new ReadinessTcpProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            case COMMAND:
                return new ReadinessCommandProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            default:
                throw new IllegalArgumentException("Unknown readiness probe type: " + probeType);
        }
    }

    public static Probe createLivenessProbe(ContainerConfiguration containerConfiguration,
    		ContainerAppsDeployerProperties containerAppsDeployerProperties, 
    		ProbeType probeType) {
        switch (probeType) {
            case HTTP:
                return new LivenessHttpProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            case TCP:
                return new LivenessTcpProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            case COMMAND:
                return new LivenessCommandProbeCreator(containerAppsDeployerProperties, containerConfiguration).create();
            default:
                throw new IllegalArgumentException("Unknown liveness probe type: " + probeType);
        }
    }
}
