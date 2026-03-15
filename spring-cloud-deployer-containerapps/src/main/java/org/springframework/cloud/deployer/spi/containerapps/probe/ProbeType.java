package org.springframework.cloud.deployer.spi.containerapps.probe;

/**
 * Defines probe types.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @since 2.5
 */
public enum ProbeType {
	HTTP,
	TCP,
	COMMAND
}
