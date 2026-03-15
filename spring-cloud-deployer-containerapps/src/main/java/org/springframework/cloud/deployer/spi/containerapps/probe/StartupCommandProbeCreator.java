package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.cloud.deployer.spi.util.CommandLineTokenizer;
import org.springframework.util.StringUtils;

/**
 * Creates a command based startup probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Corneil du Plessis
 * @since 2.5
 */
class StartupCommandProbeCreator extends CommandProbeCreator {
    StartupCommandProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties, ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    int getInitialDelay() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeDelay",
                getKubernetesDeployerProperties().getStartupCommandProbeDelay());
    }

    @Override
    int getPeriod() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbePeriod",
                getKubernetesDeployerProperties().getStartupCommandProbePeriod());
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeFailure",
                getKubernetesDeployerProperties().getLivenessCommandProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeSuccess",
                getKubernetesDeployerProperties().getLivenessCommandProbeSuccess());
    }

    @Override
    String[] getCommand() {
        String probeCommandValue = getDeploymentPropertyValue(STARTUP_DEPLOYER_PROPERTY_PREFIX + "CommandProbeCommand");

        if (StringUtils.hasText(probeCommandValue)) {
            return new CommandLineTokenizer(probeCommandValue).getArgs().toArray(new String[0]);
        }

        if (getKubernetesDeployerProperties().getStartupCommandProbeCommand() != null) {
            return new CommandLineTokenizer(getKubernetesDeployerProperties().getStartupCommandProbeCommand())
                    .getArgs().toArray(new String[0]);
        }

        throw new IllegalArgumentException("The startupCommandProbeCommand property must be set.");
    }
}
