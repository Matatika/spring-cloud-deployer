package org.springframework.cloud.deployer.spi.containerapps;

import com.azure.resourcemanager.appcontainers.models.JobTemplate;
import com.azure.resourcemanager.appcontainers.models.Template;

/**
 * Defines how a Container app {@link Container} is created.
 *
 * @author Aaron Phethean
 */
public interface ContainerFactory {
	/**
	 * Creates a {@link Template} using configuration from the provided {@link ContainerConfiguration}.
	 *
	 * @param containerConfiguration the {@link ContainerConfiguration}
	 * @return a {@link Template}
	 */
	Template createAppTemplate(ContainerConfiguration containerConfiguration);

	/**
	 * Creates a {@link JobTemplate} using configuration from the provided {@link ContainerConfiguration}.
	 *
	 * @param containerConfiguration the {@link ContainerConfiguration}
	 * @return a {@link Template}
	 */
	JobTemplate createJobTemplate(ContainerConfiguration containerConfiguration);

}
