package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.util.StringUtils;

/**
 * Creates an HTTP Startup Probe.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Corneil du Plessis
 */
class StartupHttpProbeCreator extends HttpProbeCreator {
    StartupHttpProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties,
                            ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    public Integer getPort() {
        String probePortValue = getProbeProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbePort");

        if (StringUtils.hasText(probePortValue)) {
            if (!probePortValue.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("StartupHttpProbeCreator must contain all digits");
            }

            return Integer.parseInt(probePortValue);
        }

        if (getKubernetesDeployerProperties().getStartupHttpProbePort() != null) {
            return getKubernetesDeployerProperties().getStartupHttpProbePort();
        }

        if (getDefaultPort() != null) {
            return getDefaultPort();
        }

        return null;
    }

    @Override
    protected String getProbePath() {
        String probePathValue = getProbeProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbePath");

        if (StringUtils.hasText(probePathValue)) {
            return probePathValue;
        }

        if (getKubernetesDeployerProperties().getStartupHttpProbePath() != null) {
            return getKubernetesDeployerProperties().getStartupHttpProbePath();
        }

        if (useBoot1ProbePath()) {
            return BOOT_1_READINESS_PROBE_PATH;
        }

        return BOOT_2_READINESS_PROBE_PATH;
    }

    @Override
    protected String getScheme() {
        String probeSchemeValue = getProbeProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeScheme");

        if (StringUtils.hasText(probeSchemeValue)) {
            return probeSchemeValue;
        }

        if (getKubernetesDeployerProperties().getStartupProbeScheme() != null) {
            return getKubernetesDeployerProperties().getStartupProbeScheme();
        }

        return DEFAULT_PROBE_SCHEME;
    }

    @Override
    protected int getTimeout() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeTimeout",
                getKubernetesDeployerProperties().getStartupHttpProbeTimeout());
    }

    @Override
    protected int getInitialDelay() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeDelay",
                getKubernetesDeployerProperties().getStartupHttpProbeDelay());
    }

    @Override
    protected int getPeriod() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbePeriod",
                getKubernetesDeployerProperties().getStartupHttpProbePeriod());
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeFailure",
                getKubernetesDeployerProperties().getStartupHttpProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(STARTUP_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeSuccess",
                getKubernetesDeployerProperties().getStartupHttpProbeSuccess());
    }
}
