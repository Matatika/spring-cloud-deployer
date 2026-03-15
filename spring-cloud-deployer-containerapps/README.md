# Spring Cloud Deployer Azure Container Apps
A [Spring Cloud Deployer](https://github.com/spring-cloud/spring-cloud-deployer) implementation for deploying long-lived streaming applications and short-lived tasks to Azure Container Apps.

* NB - this implementation currently only provides limited support for Tasks


## Building

Build the project without running tests using:

```
./mvnw clean install -DskipTests
```

## Tests

Run unit tests using:

```
./mvnw test
```

## Integration tests

The integration tests require an Azure Container Apps Environment.


#### Running the tests

Once the test environment has been created, you can run login with the az client and run all integration tests.

```
az login
```

Now run the tests:

```
export AZURE_SUBSCRIPTION_ID=[your subscription id]
export AZURE_TOKEN_ID=[your token id]
export SPRING_CLOUD_DEPLOYER_CONTAINERAPPS_RESOURCE_GROUP=[your resource group]
export SPRING_CLOUD_DEPLOYER_CONTAINERAPPS_ENVIRONMENT=[the container app environment name]
export REGISTRY_PASSWORD=[your registry with a matatika shelltask image]

# not required - will fallback to container app environment defaults
export SPRING_CLOUD_DEPLOYER_CONTAINERAPPS_LOCATION=[your Azure region]
export SPRING_CLOUD_DEPLOYER_CONTAINERAPPS_LOG_ANALYTICS_WORKSPACE_ID=[the workspace id of the log analytics workspace]

./mvnw verify -P failsafe
```

NOTE: if you get authentication errors, try setting the Azure SDK basic auth credentials:

https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/identity/azure-identity#environment-variables

```bash
export AZURE_CLIENT_ID=[ID of a Microsoft Entra application]
export AZURE_TENANT_ID=[(optional) ID of the application's Microsoft Entra tenant]
export AZURE_USERNAME=[a username (usually an email address)]
export AZURE_PASSWORD=[that user's password]
```



