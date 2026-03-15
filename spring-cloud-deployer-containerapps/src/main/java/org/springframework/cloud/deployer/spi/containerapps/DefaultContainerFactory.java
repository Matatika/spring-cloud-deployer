package org.springframework.cloud.deployer.spi.containerapps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.ContainerResources;
import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.azure.resourcemanager.appcontainers.models.JobTemplate;
import com.azure.resourcemanager.appcontainers.models.Template;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Create a Container Apps {@link Template} defining the image and resources that 
 * will be started as part of launching App.
 *
 * @author Aaron Phethean
 */
public class DefaultContainerFactory implements ContainerFactory {
    private static Log logger = LogFactory.getLog(DefaultContainerFactory.class);
    private static final String SPRING_APPLICATION_JSON = "SPRING_APPLICATION_JSON";
//    private static final String SPRING_CLOUD_APPLICATION_GUID = "SPRING_CLOUD_APPLICATION_GUID";

    private final ContainerAppsDeployerProperties properties;

    public DefaultContainerFactory(ContainerAppsDeployerProperties properties) {
        this.properties = properties;
    }

    @Override
    public JobTemplate createJobTemplate(ContainerConfiguration containerConfiguration) {
        AppDeploymentRequest request = containerConfiguration.getAppDeploymentRequest();
        Map<String, String> deploymentProperties = getDeploymentProperties(request);
        DeploymentPropertiesResolver deploymentPropertiesResolver = getDeploymentPropertiesResolver(request);

        String image;
        try {
            image = request.getResource().getURI().getSchemeSpecificPart();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
        }
        logger.info("Using Docker image: " + image);

        EntryPointStyle entryPointStyle = deploymentPropertiesResolver.determineEntryPointStyle(deploymentProperties);
        logger.info("Using Docker entry point style: " + entryPointStyle);

        Map<String, String> envVarsMap = new HashMap<>();
        for (String envVar : this.properties.getEnvironmentVariables()) {
            String[] strings = envVar.split("=", 2);
            Assert.isTrue(strings.length == 2, "Invalid environment variable declared: " + envVar);
            envVarsMap.put(strings[0], strings[1]);
        }
        //Create EnvVar entries for additional variables set at the app level
        //For instance, this may be used to set JAVA_OPTS independently for each app if the base container
        //image supports it.
        envVarsMap.putAll(deploymentPropertiesResolver.getAppEnvironmentVariables(deploymentProperties));

        List<String> appArgs = new ArrayList<>();

        Map<String, String> appAdminCredentials = new HashMap<>();
        properties.getAppAdmin().addCredentialsToAppEnvironmentAsProperties(appAdminCredentials);

        switch (entryPointStyle) {
            case exec:
                appArgs = createCommandArgs(request);
                List<String> finalAppArgs = appArgs;
                appAdminCredentials.forEach((k, v) -> finalAppArgs.add(String.format("--%s=%s", k, v)));


                break;
            case boot:
                if (envVarsMap.containsKey(SPRING_APPLICATION_JSON)) {
                    throw new IllegalStateException(
                            "You can't use boot entry point style and also set SPRING_APPLICATION_JSON for the app");
                }
                try {
                    envVarsMap.put(SPRING_APPLICATION_JSON,
                            new ObjectMapper().writeValueAsString(request.getDefinition().getProperties()));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Unable to create SPRING_APPLICATION_JSON", e);
                }

                appArgs = request.getCommandlineArguments();

                break;
            case shell:
                for (String key : request.getDefinition().getProperties().keySet()) {
                    String envVar = key.replace('.', '_').toUpperCase();
                    envVarsMap.put(envVar, request.getDefinition().getProperties().get(key));
                    envVarsMap.putAll(appAdminCredentials);

                }
                // Push all the command line arguments as environment properties
                // The task app name(in case of Composed Task), platform_name and executionId are expected to be updated.
                // This will also override any of the existing app properties that match the provided cmdline args.
                for (String cmdLineArg : request.getCommandlineArguments()) {
                    String cmdLineArgKey;

                    if (cmdLineArg.startsWith("--")) {
                        cmdLineArgKey = cmdLineArg.substring(2, cmdLineArg.indexOf("="));
                    } else {
                        cmdLineArgKey = cmdLineArg.substring(0, cmdLineArg.indexOf("="));
                    }

                    String cmdLineArgValue = cmdLineArg.substring(cmdLineArg.indexOf("=") + 1);
                    envVarsMap.put(cmdLineArgKey.replace('.', '_').toUpperCase(), cmdLineArgValue);
                }
                break;
        }

        List<EnvironmentVar> envVars = new ArrayList<>();
        for (Map.Entry<String, String> e : envVarsMap.entrySet()) {
            envVars.add(new EnvironmentVar()
            		.withName(e.getKey())
            		.withValue(e.getValue()));
        }

        envVars.addAll(deploymentPropertiesResolver.getSecretKeyRefs(deploymentProperties));
        envVars.addAll(deploymentPropertiesResolver.getConfigMapKeyRefs(deploymentProperties));
//        envVars.add(getGUIDEnvVar());

        if (request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY) != null) {
            envVars.add(new EnvironmentVar()
            		.withName("SPRING_CLOUD_APPLICATION_GROUP")
            		.withValue(request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY)));
        }

//        List<EnvFromSource> envFromSources = new ArrayList<>();
//        envFromSources.addAll(deploymentPropertiesResolver.getConfigMapRefs(deploymentProperties));
//        envFromSources.addAll(deploymentPropertiesResolver.getSecretRefs(deploymentProperties));

        Container container = new Container()
        		.withName(containerConfiguration.getAppId())
        		.withImage(image)
        		.withEnv(envVars)
        		.withArgs(appArgs)
                .withVolumeMounts(deploymentPropertiesResolver.getVolumeMounts(deploymentProperties))
                .withResources(new ContainerResources().withCpu(2.0).withMemory("4Gi"));

        // Override the containers default entry point with one specified during the app deployment
        List<String> containerCommand = deploymentPropertiesResolver.getContainerCommand(deploymentProperties);
        if (!containerCommand.isEmpty()) {
            container.withCommand(containerCommand);
        }

        List<Container> allContainers = new ArrayList<>();
        allContainers.add(container);
        allContainers.addAll(deploymentPropertiesResolver.getAdditionalContainers(deploymentProperties));
        JobTemplate template = new JobTemplate()
        		.withContainers(allContainers);
//        		.withVolumes(deploymentPropertiesResolver.getVolumes(deploymentProperties));

        return template;
    }

    @Override
    public Template createAppTemplate(ContainerConfiguration containerConfiguration) {
        AppDeploymentRequest request = containerConfiguration.getAppDeploymentRequest();
        Map<String, String> deploymentProperties = getDeploymentProperties(request);
        DeploymentPropertiesResolver deploymentPropertiesResolver = getDeploymentPropertiesResolver(request);

        String image;
        try {
            image = request.getResource().getURI().getSchemeSpecificPart();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
        }
        logger.info("Using Docker image: " + image);

        EntryPointStyle entryPointStyle = deploymentPropertiesResolver.determineEntryPointStyle(deploymentProperties);
        logger.info("Using Docker entry point style: " + entryPointStyle);

        Map<String, String> envVarsMap = new HashMap<>();
        for (String envVar : this.properties.getEnvironmentVariables()) {
            String[] strings = envVar.split("=", 2);
            Assert.isTrue(strings.length == 2, "Invalid environment variable declared: " + envVar);
            envVarsMap.put(strings[0], strings[1]);
        }
        //Create EnvVar entries for additional variables set at the app level
        //For instance, this may be used to set JAVA_OPTS independently for each app if the base container
        //image supports it.
        envVarsMap.putAll(deploymentPropertiesResolver.getAppEnvironmentVariables(deploymentProperties));

        List<String> appArgs = new ArrayList<>();

        Map<String, String> appAdminCredentials = new HashMap<>();
        properties.getAppAdmin().addCredentialsToAppEnvironmentAsProperties(appAdminCredentials);

        switch (entryPointStyle) {
            case exec:
                appArgs = createCommandArgs(request);
                List<String> finalAppArgs = appArgs;
                appAdminCredentials.forEach((k, v) -> finalAppArgs.add(String.format("--%s=%s", k, v)));


                break;
            case boot:
                if (envVarsMap.containsKey(SPRING_APPLICATION_JSON)) {
                    throw new IllegalStateException(
                            "You can't use boot entry point style and also set SPRING_APPLICATION_JSON for the app");
                }
                try {
                    envVarsMap.put(SPRING_APPLICATION_JSON,
                            new ObjectMapper().writeValueAsString(request.getDefinition().getProperties()));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Unable to create SPRING_APPLICATION_JSON", e);
                }

                appArgs = request.getCommandlineArguments();

                break;
            case shell:
                for (String key : request.getDefinition().getProperties().keySet()) {
                    String envVar = key.replace('.', '_').toUpperCase();
                    envVarsMap.put(envVar, request.getDefinition().getProperties().get(key));
                    envVarsMap.putAll(appAdminCredentials);

                }
                // Push all the command line arguments as environment properties
                // The task app name(in case of Composed Task), platform_name and executionId are expected to be updated.
                // This will also override any of the existing app properties that match the provided cmdline args.
                for (String cmdLineArg : request.getCommandlineArguments()) {
                    String cmdLineArgKey;

                    if (cmdLineArg.startsWith("--")) {
                        cmdLineArgKey = cmdLineArg.substring(2, cmdLineArg.indexOf("="));
                    } else {
                        cmdLineArgKey = cmdLineArg.substring(0, cmdLineArg.indexOf("="));
                    }

                    String cmdLineArgValue = cmdLineArg.substring(cmdLineArg.indexOf("=") + 1);
                    envVarsMap.put(cmdLineArgKey.replace('.', '_').toUpperCase(), cmdLineArgValue);
                }
                break;
        }

        List<EnvironmentVar> envVars = new ArrayList<>();
        for (Map.Entry<String, String> e : envVarsMap.entrySet()) {
            envVars.add(new EnvironmentVar()
            		.withName(e.getKey())
            		.withValue(e.getValue()));
        }

        envVars.addAll(deploymentPropertiesResolver.getSecretKeyRefs(deploymentProperties));
        envVars.addAll(deploymentPropertiesResolver.getConfigMapKeyRefs(deploymentProperties));
//        envVars.add(getGUIDEnvVar());

        if (request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY) != null) {
            envVars.add(new EnvironmentVar()
            		.withName("SPRING_CLOUD_APPLICATION_GROUP")
            		.withValue(request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY)));
        }

//        List<EnvFromSource> envFromSources = new ArrayList<>();
//        envFromSources.addAll(deploymentPropertiesResolver.getConfigMapRefs(deploymentProperties));
//        envFromSources.addAll(deploymentPropertiesResolver.getSecretRefs(deploymentProperties));

        Container container = new Container()
        		.withName(containerConfiguration.getAppId())
        		.withImage(image)
        		.withEnv(envVars)
        		.withArgs(appArgs)
        		.withVolumeMounts(deploymentPropertiesResolver.getVolumeMounts(deploymentProperties));

        Template template = new Template()
        		.withContainers(Arrays.asList(container));
//        		.withVolumes(deploymentPropertiesResolver.getVolumes(deploymentProperties));

        Set<Integer> ports = new HashSet<>();

        Integer defaultPort = containerConfiguration.getExternalPort();

        if (defaultPort != null) {
            ports.add(defaultPort);
        }

        ports.addAll(deploymentPropertiesResolver.getContainerPorts(deploymentProperties));
//        configureStartupProbe(containerConfiguration, container, ports);
//        configureReadinessProbe(containerConfiguration, container, ports);
//        configureLivenessProbe(containerConfiguration, container, ports);

        if (!ports.isEmpty()) {
            for (Integer containerPort : ports) {
            	throw new RuntimeException(String.format("Cannot bind port %s, not supported by container apps", containerPort));
            }
        }

        //Override the containers default entry point with one specified during the app deployment
        List<String> containerCommand = deploymentPropertiesResolver.getContainerCommand(deploymentProperties);
        if (!containerCommand.isEmpty()) {
            container.withCommand(containerCommand);
        }
        return template;
    }

    /**
     * Create command arguments
     *
     * @param request the {@link AppDeploymentRequest}
     * @return the command line arguments to use
     */
    List<String> createCommandArgs(AppDeploymentRequest request) {
        List<String> cmdArgs = new LinkedList<>();

        List<String> commandArgOptions = request.getCommandlineArguments().stream()
                .map(this::getArgOption)
                .collect(Collectors.toList());

        // add properties from deployment request
        Map<String, String> args = request.getDefinition().getProperties();
        for (Map.Entry<String, String> entry : args.entrySet()) {
            if (!StringUtils.hasText(entry.getValue())) {
                logger.warn(
                        "Excluding request property with missing value from command args: " + entry.getKey());
            } else if (commandArgOptions.contains(entry.getKey())) {
                logger.warn(
                        String.format(
                                "Excluding request property [--%s=%s] as a command arg. Existing command line argument takes precedence."
                                , entry.getKey(), entry.getValue()));
            } else {
                cmdArgs.add(String.format("--%s=%s", entry.getKey(), entry.getValue()));
            }
        }
        // add provided command line args
        cmdArgs.addAll(request.getCommandlineArguments());
        logger.debug("Using command args: " + cmdArgs);
        return cmdArgs;
    }

    private String getArgOption(String arg) {
        int indexOfAssignment = arg.indexOf("=");
        String argOption = (indexOfAssignment < 0) ? arg : arg.substring(0, indexOfAssignment);
        return argOption.trim().replaceAll("^--", "");
    }

    private DeploymentPropertiesResolver getDeploymentPropertiesResolver(AppDeploymentRequest request) {
    	if (request instanceof ScheduleRequest)
    		throw new RuntimeException("Not implemented");
//        String propertiesPrefix = (request instanceof ScheduleRequest &&
//                ((ScheduleRequest) request).getSchedulerProperties() != null &&
//                ((ScheduleRequest) request).getSchedulerProperties().size() > 0) ? KubernetesSchedulerProperties.KUBERNETES_SCHEDULER_PROPERTIES_PREFIX :
//                ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX;
    	String propertiesPrefix = ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX;
        return new DeploymentPropertiesResolver(propertiesPrefix, this.properties);
    }

    private Map<String, String> getDeploymentProperties(AppDeploymentRequest request) {
        return request.getDeploymentProperties();
    }
}
