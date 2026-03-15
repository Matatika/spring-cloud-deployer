package org.springframework.cloud.deployer.spi.containerapps.probe;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.util.StringUtils;

/**
 * Creates an HTTP Liveness probe
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class LivenessHttpProbeCreator extends HttpProbeCreator {
    LivenessHttpProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties,
                             ContainerConfiguration containerConfiguration) {
        super(containerAppsDeployerProperties, containerConfiguration);
    }

    @Override
    public Integer getPort() {
        String probePortValue = getProbeProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbePort");

        if (StringUtils.hasText(probePortValue)) {
            return Integer.parseInt(probePortValue);
        }

        if (getKubernetesDeployerProperties().getLivenessHttpProbePort() != null) {
            return getKubernetesDeployerProperties().getLivenessHttpProbePort();
        }

        if (getDefaultPort() != null) {
            return getDefaultPort();
        }

        return null;
    }

    @Override
    protected String getProbePath() {
        String probePathValue = getProbeProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbePath",
                getKubernetesDeployerProperties().getLivenessHttpProbePath());

        if (StringUtils.hasText(probePathValue)) {
            return probePathValue;
        }

        if (useBoot1ProbePath()) {
            return BOOT_1_LIVENESS_PROBE_PATH;
        }

        return BOOT_2_LIVENESS_PROBE_PATH;
    }

    @Override
    protected String getScheme() {
        String probeSchemeValue = getProbeProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeScheme",
                getKubernetesDeployerProperties().getLivenessHttpProbeScheme());

        if (StringUtils.hasText(probeSchemeValue)) {
            return probeSchemeValue;
        }

        return DEFAULT_PROBE_SCHEME;
    }

    @Override
    protected int getTimeout() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeTimeout",
                getKubernetesDeployerProperties().getLivenessHttpProbeTimeout());
    }

    @Override
    protected int getInitialDelay() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeDelay",
                getKubernetesDeployerProperties().getLivenessHttpProbeDelay());
    }

    @Override
    protected int getPeriod() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbePeriod",
                getKubernetesDeployerProperties().getLivenessHttpProbePeriod());
    }

    @Override
    int getFailure() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeFailure",
                getKubernetesDeployerProperties().getLivenessHttpProbeFailure());
    }

    @Override
    int getSuccess() {
        return getProbeIntProperty(LIVENESS_DEPLOYER_PROPERTY_PREFIX, "Http", "ProbeSuccess",
                getKubernetesDeployerProperties().getLivenessHttpProbeSuccess());
    }
}
