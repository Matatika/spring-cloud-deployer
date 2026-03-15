package org.springframework.cloud.deployer.spi.containerapps.probe;

import java.util.Map;

import org.springframework.cloud.deployer.spi.containerapps.ContainerAppsDeployerProperties;
import org.springframework.cloud.deployer.spi.containerapps.ContainerConfiguration;
import org.springframework.cloud.deployer.spi.containerapps.support.PropertyParserUtils;
import org.springframework.util.StringUtils;

import io.fabric8.kubernetes.api.model.Probe;

/**
 * Base class for creating Probe's
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Ilayaperumal Gopinathan
 */
abstract class ProbeCreator {
    static final String LIVENESS_DEPLOYER_PROPERTY_PREFIX = ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX + ".liveness";
    static final String READINESS_DEPLOYER_PROPERTY_PREFIX = ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX + ".readiness";
    static final String STARTUP_DEPLOYER_PROPERTY_PREFIX = ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX + ".startup";

    private ContainerConfiguration containerConfiguration;
    private ContainerAppsDeployerProperties containerAppsDeployerProperties;

    ProbeCreator(ContainerAppsDeployerProperties containerAppsDeployerProperties,
                 ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        this.containerAppsDeployerProperties = containerAppsDeployerProperties;
    }

    abstract Probe create();

    abstract int getInitialDelay();

    abstract int getPeriod();

    abstract int getFailure();

    abstract int getSuccess();

    ContainerAppsDeployerProperties getKubernetesDeployerProperties() {
        return containerAppsDeployerProperties;
    }

    private Map<String, String> getDeploymentProperties() {
        return this.containerConfiguration.getAppDeploymentRequest().getDeploymentProperties();
    }

    protected String getDeploymentPropertyValue(String propertyName) {
        return PropertyParserUtils.getDeploymentPropertyValue(getDeploymentProperties(), propertyName);
    }
    protected String getDeploymentPropertyValue(String propertyName, String defaultValue) {
        return PropertyParserUtils.getDeploymentPropertyValue(getDeploymentProperties(), propertyName, defaultValue);
    }

    ContainerConfiguration getContainerConfiguration() {
        return containerConfiguration;
    }

    // used to resolve deprecated HTTP probe property names that do not include "Http" in them
    // can be removed when deprecated HTTP probes without "Http" in them get removed
    String getProbeProperty(String propertyPrefix, String probeName, String propertySuffix) {
        String defaultValue = getDeploymentPropertyValue(propertyPrefix + probeName + propertySuffix);
        return StringUtils.hasText(defaultValue) ? defaultValue :
                getDeploymentPropertyValue(propertyPrefix + propertySuffix);
    }

    String getProbeProperty(String propertyPrefix, String probeName, String propertySuffix, String defaultValue) {
        return getDeploymentPropertyValue(propertyPrefix + probeName + propertySuffix,
                getDeploymentPropertyValue(propertyPrefix + propertySuffix, defaultValue)
        );
    }
    int getProbeIntProperty(String propertyPrefix, String probeName, String propertySuffix, int defaultValue) {
        String propertyValue = getDeploymentPropertyValue(propertyPrefix + probeName + propertySuffix,
                getDeploymentPropertyValue(propertyPrefix + propertySuffix)
        );
        return StringUtils.hasText(propertyValue) ? Integer.parseInt(propertyValue) : defaultValue;
    }
}
