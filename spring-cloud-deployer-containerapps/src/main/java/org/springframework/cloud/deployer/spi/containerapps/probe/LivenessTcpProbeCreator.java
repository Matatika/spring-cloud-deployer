package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.util.StringUtils;

/**
 * Creates a TCP liveness probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Corneil du Plessis
 * @since 2.5
 */
class LivenessTcpProbeCreator extends TcpProbeCreator {
    LivenessTcpProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties, ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    int getInitialDelay() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeDelay",
                getKubernetesDeployerProperties().getLivenessTcpProbeDelay());
    }

    @Override
    int getPeriod() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbePeriod",
                getKubernetesDeployerProperties().getLivenessTcpProbePeriod());
    }

    @Override
    protected int getTimeout() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeTimeout",
                getKubernetesDeployerProperties().getLivenessTcpProbeTimeout());
    }

    @Override
    Integer getPort() {
        String probePortValue = getProbeProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbePort");

        if (StringUtils.hasText(probePortValue)) {
            if (!probePortValue.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("LivenessTcpProbePort must contain all digits");
            }

            return Integer.parseInt(probePortValue);
        }

        if (getKubernetesDeployerProperties().getLivenessTcpProbePort() != null) {
            return getKubernetesDeployerProperties().getLivenessTcpProbePort();
        }

        throw new IllegalArgumentException("The livenessTcpProbePort property must be set.");
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeFailure",
                getKubernetesDeployerProperties().getLivenessTcpProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeSuccess",
                getKubernetesDeployerProperties().getLivenessTcpProbeSuccess());
    }
}
