package org.springframework.cloud.deployer.spi.containerapps;

/**
 * Defines supported authentication types to use when accessing secured
 * probe endpoints.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 */
public enum ProbeAuthenticationType {
	Basic
}
