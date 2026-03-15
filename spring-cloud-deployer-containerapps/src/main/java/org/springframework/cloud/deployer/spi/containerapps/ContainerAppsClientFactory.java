package org.springframework.cloud.deployer.spi.containerapps;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.LogsQueryClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;

/**
 * The class responsible for creating AZ Container Apps Client based on the deployer properties.
 *
 * @author Aaron Phethean
 */
public class ContainerAppsClientFactory {

	private static AzureProfile getAzureProfile() {
		/*
		 * Must be set in environment
		 * AZURE_TENANT_ID
		 * AZURE_SUBSCRIPTION_ID
		 */
		AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
		return profile;
	}

	private static TokenCredential getTokenCredential(AzureProfile profile) {
		/*
		 * Attempt to lookup the credentials from our environment and other mechanisms available during development
		 * Environment - The DefaultAzureCredential will read account information specified via environment variables and use it to authenticate.
		 * Workload Identity - If the app is deployed on Kubernetes with environment variables set by the workload identity webhook, DefaultAzureCredential will authenticate the configured identity.
		 * Managed Identity - If the application is deployed to an Azure host with Managed Identity enabled, the DefaultAzureCredential will authenticate with that account.
		 * Azure Developer CLI - If the developer has authenticated an account via the Azure Developer CLI azd auth login command, the DefaultAzureCredential will authenticate with that account.
		 * IntelliJ - If the developer has authenticated via Azure Toolkit for IntelliJ, the DefaultAzureCredential will authenticate with that account.
		 * Azure CLI - If the developer has authenticated an account via the Azure CLI az login command, the DefaultAzureCredential will authenticate with that account.
		 * Azure PowerShell
		 * https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity/azure-identity#environment-variables
		 */
		TokenCredential credential = new DefaultAzureCredentialBuilder()
				.authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
				.build();
		return credential;
	}

	public static ContainerAppsApiClient getContainerAppsApiClient(ContainerAppsDeployerProperties deployerProperties) {
		AzureProfile profile = getAzureProfile();
		TokenCredential credential = getTokenCredential(profile);
		ContainerAppsApiManager manager = ContainerAppsApiManager
				.authenticate(credential, profile);
		return manager.serviceClient();
	}

	public static LogsQueryClient getLogsQueryClient(ContainerAppsDeployerProperties deployerProperties) {
		AzureProfile profile = getAzureProfile();
		TokenCredential credential = getTokenCredential(profile);
		LogsQueryClient logsQueryClient = new LogsQueryClientBuilder()
			    .credential(credential)
			    .buildClient();
		return logsQueryClient;
	}
}
