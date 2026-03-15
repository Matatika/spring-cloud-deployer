package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.cloud.deployer.spi.util.CommandLineTokenizer;
import org.springframework.util.StringUtils;

/**
 * Creates a command based readiness probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Corneil du Plessis
 * @since 2.5
 */
class ReadinessCommandProbeCreator extends CommandProbeCreator {
    ReadinessCommandProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties, ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    int getInitialDelay() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeDelay",
                getKubernetesDeployerProperties().getReadinessCommandProbeDelay());
    }

    @Override
    int getPeriod() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbePeriod",
                getKubernetesDeployerProperties().getReadinessCommandProbePeriod());
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeFailure",
                getKubernetesDeployerProperties().getReadinessCommandProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(READINESS_DEPLOYER_PROPERTY_PREFIX, "Command", "ProbeSuccess",
                getKubernetesDeployerProperties().getReadinessCommandProbeSuccess());
    }

    @Override
    String[] getCommand() {
        String probeCommandValue = getDeploymentPropertyValue(READINESS_DEPLOYER_PROPERTY_PREFIX + "CommandProbeCommand");

        if (StringUtils.hasText(probeCommandValue)) {
            return new CommandLineTokenizer(probeCommandValue).getArgs().toArray(new String[0]);
        }

        if (getKubernetesDeployerProperties().getReadinessCommandProbeCommand() != null) {
            return new CommandLineTokenizer(getKubernetesDeployerProperties().getReadinessCommandProbeCommand())
                    .getArgs().toArray(new String[0]);
        }

        throw new IllegalArgumentException("The readinessCommandProbeCommand property must be set.");
    }
}
