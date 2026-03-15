package org.springframework.cloud.deployer.spi.containerapps;

/**
 * Defines identity types that are available.
 * https://learn.microsoft.com/en-us/azure/container-apps/managed-identity
 *
 * @author Aaron Phethean
 */
public enum IdentityType {
	/**
	 * Containers are run with no Azure identity.
	 */
	None,

	/**
	 * Containers are run with a System Azure identity of the resource.
	 */
	SystemAssigned,

	/**
	 * Containers are run with user defined Azure identities.
	 */
	UserAssigned
}
