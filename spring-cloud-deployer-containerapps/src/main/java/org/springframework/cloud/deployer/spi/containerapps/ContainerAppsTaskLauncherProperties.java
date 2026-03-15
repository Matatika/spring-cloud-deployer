package org.springframework.cloud.deployer.spi.containerapps;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Container Apps Task Launcher.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties(prefix = "spring.cloud.deployer.containerapps")
public class ContainerAppsTaskLauncherProperties {
	/**
	 * The {@link RestartPolicy} to use. Defaults to {@link RestartPolicy#Never}.
	 */
	private RestartPolicy restartPolicy = RestartPolicy.Never;

	/**
	 * The backoff limit to specify the number of retries before considering a Job as failed.
	 */
	private Integer backoffLimit;

	/**
	 * The number of seconds after a job has finished before it is eligible to be automatically
	 * removed by the TTL controller - note that logs from removed jobs will not be able to
	 * be retrieved.
	 */
	private Integer ttlSecondsAfterFinished;

	/**
	 * Obtains the {@link RestartPolicy} to use. Defaults to
	 * {@link ContainerAppsTaskLauncherProperties#restartPolicy}.
	 *
	 * @return the {@link RestartPolicy} to use
	 */
	public RestartPolicy getRestartPolicy() {
		return restartPolicy;
	}

	/**
	 * Sets the {@link RestartPolicy} to use.
	 *
	 * @param restartPolicy the {@link RestartPolicy} to use
	 */
	public void setRestartPolicy(RestartPolicy restartPolicy) {
		this.restartPolicy = restartPolicy;
	}

	/**
	 * Get the BackoffLimit value
	 * @return the integer value of BackoffLimit
	 */
	public Integer getBackoffLimit() {
		return backoffLimit;
	}

	/**
	 * Sets the BackoffLimit.
	 *
	 * @param backoffLimit the integer value of BackoffLimit
	 */
	public void setBackoffLimit(Integer backoffLimit) {
		this.backoffLimit = backoffLimit;
	}

	/**
	 * Get the ttlSecondsAfterFinished value
	 * @return the integer value of ttlSecondsAfterFinished
	 */
	public Integer getTtlSecondsAfterFinished() {
		return ttlSecondsAfterFinished;
	}

	/**
	 * Sets the ttlSecondsAfterFinished.
	 *
	 * @param ttlSecondsAfterFinished the integer value of ttlSecondsAfterFinished
	 */
	public void setTtlSecondsAfterFinished(Integer ttlSecondsAfterFinished) {
		this.ttlSecondsAfterFinished = ttlSecondsAfterFinished;
	}
}
