package org.springframework.cloud.deployer.spi.containerapps;

/**
 * Defines restart policies that are available.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 */
public enum RestartPolicy {
	/**
	 * Always restart a failed container.
	 */
	Always,

	/**
	 * Restart a failed container with an exponential back-off delay, capped at 5 minutes.
	 */
	OnFailure,

	/**
	 * Never restarts a successful or failed container.
	 */
	Never
}
