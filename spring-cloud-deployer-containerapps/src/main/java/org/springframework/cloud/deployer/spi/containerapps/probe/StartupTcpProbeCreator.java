package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.util.StringUtils;

/**
 * Creates a TCP startup probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Corneil du Plessis
 * @since 2.5
 */
class StartupTcpProbeCreator extends TcpProbeCreator {
    StartupTcpProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties, ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    int getInitialDelay() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeDelay",
                getKubernetesDeployerProperties().getStartupTcpProbeDelay());
    }

    @Override
    int getPeriod() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbePeriod",
                getKubernetesDeployerProperties().getStartupTcpProbePeriod());
    }

    @Override
    protected int getTimeout() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeTimeout",
                getKubernetesDeployerProperties().getStartupTcpProbeTimeout());
    }

    @Override
    Integer getPort() {
        String probePortValue = getProbeProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbePort");

        if (StringUtils.hasText(probePortValue)) {
            if (!probePortValue.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("StartupTcpProbePort must contain all digits");
            }

            return Integer.parseInt(probePortValue);
        }

        if (getKubernetesDeployerProperties().getStartupTcpProbePort() != null) {
            return getKubernetesDeployerProperties().getStartupTcpProbePort();
        }

        throw new IllegalArgumentException("A startupTcpProbePort property must be set.");
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeFailure",
                getKubernetesDeployerProperties().getStartupTcpProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Tcp", "ProbeSuccess",
                getKubernetesDeployerProperties().getStartupTcpProbeSuccess());
    }
}
