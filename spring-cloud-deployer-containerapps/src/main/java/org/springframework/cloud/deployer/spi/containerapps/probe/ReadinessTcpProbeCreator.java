package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.util.StringUtils;

/**
 * Creates a TCP readiness probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Corneil du Plessis
 * @since 2.5
 */
class ReadinessTcpProbeCreator extends TcpProbeCreator {
    ReadinessTcpProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties, ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    int getInitialDelay() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeDelay",
                getKubernetesDeployerProperties().getReadinessTcpProbeDelay());
    }

    @Override
    int getPeriod() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbePeriod",
                getKubernetesDeployerProperties().getReadinessTcpProbePeriod());
    }

    @Override
    protected int getTimeout() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeTimeout",
                getKubernetesDeployerProperties().getReadinessTcpProbeTimeout());
    }

    @Override
    Integer getPort() {
        String probePortValue = getProbeProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbePort");

        if (StringUtils.hasText(probePortValue)) {
            if (!probePortValue.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("ReadinessTcpProbePort must contain all digits");
            }

            return Integer.parseInt(probePortValue);
        }

        if (getKubernetesDeployerProperties().getReadinessTcpProbePort() != null) {
            return getKubernetesDeployerProperties().getReadinessTcpProbePort();
        }

        throw new IllegalArgumentException("A readinessTcpProbePort property must be set.");
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeFailure",
                getKubernetesDeployerProperties().getReadinessTcpProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeSuccess",
                getKubernetesDeployerProperties().getReadinessTcpProbeSuccess());
    }
}
