package org.springframework.cloud.deployer.spi.containerapps;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

import io.fabric8.kubernetes.api.model.Secret;

/**
 * Encapsulates parameters used to configure a container.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 */
public class ContainerConfiguration {
	private String appId;
	private Integer externalPort;
	private boolean isHostNetwork;
	private Secret probeCredentialsSecret;
	private AppDeploymentRequest appDeploymentRequest;

	public ContainerConfiguration(String appId, AppDeploymentRequest appDeploymentRequest) {
		this.appId = appId;
		this.appDeploymentRequest = appDeploymentRequest;
	}

	public AppDeploymentRequest getAppDeploymentRequest() {
		return appDeploymentRequest;
	}

	public String getAppId() {
		return appId;
	}

	public boolean isHostNetwork() {
		return isHostNetwork;
	}

	public ContainerConfiguration withHostNetwork(boolean isHostNetwork) {
		this.isHostNetwork = isHostNetwork;
		return this;
	}

	public ContainerConfiguration withExternalPort(Integer externalPort) {
		this.externalPort = externalPort;
		return this;
	}

	public Integer getExternalPort() {
		return externalPort;
	}

	public ContainerConfiguration withProbeCredentialsSecret(Secret probeCredentialsSecret) {
		this.probeCredentialsSecret = probeCredentialsSecret;
		return this;
	}

	public Secret getProbeCredentialsSecret() {
		return probeCredentialsSecret;
	}
}
