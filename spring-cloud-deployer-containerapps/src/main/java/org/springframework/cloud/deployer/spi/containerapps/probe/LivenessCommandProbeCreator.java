package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.cloud.deployer.spi.util.CommandLineTokenizer;
import org.springframework.util.StringUtils;

/**
 * Creates a command based liveness probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Corneil du Plessis
 * @since 2.5
 */
class LivenessCommandProbeCreator extends CommandProbeCreator {
    LivenessCommandProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties, ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    int getInitialDelay() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeDelay",
                getKubernetesDeployerProperties().getLivenessCommandProbeDelay());
    }

    @Override
    int getPeriod() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbePeriod",
                getKubernetesDeployerProperties().getLivenessCommandProbePeriod());
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeFailure",
                getKubernetesDeployerProperties().getLivenessCommandProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeSuccess",
                getKubernetesDeployerProperties().getLivenessCommandProbeSuccess());
    }

    @Override
    String[] getCommand() {
        String probeCommandValue = getDeploymentPropertyValue(LIVENESS_DEPLOYER_PROPERTY_PREFIX + "CommandProbeCommand");

        if (StringUtils.hasText(probeCommandValue)) {
            return new CommandLineTokenizer(probeCommandValue).getArgs().toArray(new String[0]);
        }

        if (getKubernetesDeployerProperties().getLivenessCommandProbeCommand() != null) {
            return new CommandLineTokenizer(getKubernetesDeployerProperties().getLivenessCommandProbeCommand())
                    .getArgs().toArray(new String[0]);
        }

        throw new IllegalArgumentException("The livenessCommandProbeCommand property must be set.");
    }
}
