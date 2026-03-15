package org.springframework.cloud.deployer.spi.containerapps;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;

/**
 * Represents the status of a module.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Florian Rosenberg
 * @author Thomas Risberg
 * @author David Turanski
 * @author Chris Schaefer
 */
public class ContainerAppsAppInstanceStatus implements AppInstanceStatus {

    private static Log logger = LogFactory.getLog(ContainerAppsAppInstanceStatus.class);

    private final Pod pod;

    private final Service service;

//    private final ContainerAppsDeployerProperties properties;

    private ContainerStatus containerStatus;

    private RunningPhaseDeploymentStateResolver runningPhaseDeploymentStateResolver;

//    @Deprecated
//    public ContainerAppsAppInstanceStatus(Pod pod, Service service, KubernetesDeployerProperties properties) {
//        this.pod = pod;
//        this.service = service;
//        this.properties = properties;
//        // we assume one container per pod
//        if (pod != null && pod.getStatus().getContainerStatuses().size() == 1) {
//            this.containerStatus = pod.getStatus().getContainerStatuses().get(0);
//        } else {
//            this.containerStatus = null;
//        }
//        this.runningPhaseDeploymentStateResolver = new DefaultRunningPhaseDeploymentStateResolver(properties);
//    }

    public ContainerAppsAppInstanceStatus(Pod pod, Service service, ContainerAppsDeployerProperties properties,
                                       ContainerStatus containerStatus) {
        this.pod = pod;
        this.service = service;
//        this.properties = properties;
        this.containerStatus = containerStatus;
        this.runningPhaseDeploymentStateResolver = new DefaultRunningPhaseDeploymentStateResolver(properties);
    }

    /**
     * Override the default {@link RunningPhaseDeploymentStateResolver} implementation.
     *
     * @param runningPhaseDeploymentStateResolver the
     *                                            {@link RunningPhaseDeploymentStateResolver} to use
     */
    public void setRunningPhaseDeploymentStateResolver(
            RunningPhaseDeploymentStateResolver runningPhaseDeploymentStateResolver) {
        this.runningPhaseDeploymentStateResolver = runningPhaseDeploymentStateResolver;
    }

    @Override
    public String getId() {
        return pod == null ? "N/A" : pod.getMetadata().getName();
    }

    @Override
    public DeploymentState getState() {
        return pod != null && containerStatus != null ? mapState() : DeploymentState.unknown;
    }

    /**
     * Maps Kubernetes phases/states onto Spring Cloud Deployer states
     */
    private DeploymentState mapState() {
        logger.debug(String.format("%s - Phase [ %s ]", pod.getMetadata().getName(), pod.getStatus().getPhase()));
        logger.debug(String.format("%s - ContainerStatus [ %s ]", pod.getMetadata().getName(), containerStatus));
        switch (pod.getStatus().getPhase()) {

            case "Pending":
                return DeploymentState.deploying;

            // We only report a module as running if the container is also ready to service requests.
            // We also implement the Readiness check as part of the container to ensure ready means
            // that the module is up and running and not only that the JVM has been created and the
            // Spring module is still starting up
            case "Running":
                // we assume we only have one container
                return runningPhaseDeploymentStateResolver.resolve(containerStatus);
            case "Failed":
                return DeploymentState.failed;

            case "Unknown":
                return DeploymentState.unknown;

            default:
                return DeploymentState.unknown;
        }
    }

    @Override
    public Map<String, String> getAttributes() {

        ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();

        if (pod != null) {
            result.put("pod.name", pod.getMetadata().getName());
            result.put("pod.startTime", nullSafe(pod.getStatus().getStartTime()));
            result.put("pod.ip", nullSafe(pod.getStatus().getPodIP()));
            result.put("actuator.path", determineActuatorPathFromLivenessProbe(pod));
            result.put("actuator.port", determineActuatorPortFromLivenessProbe(pod, result.get("actuator.path")));
            result.put("host.ip", nullSafe(pod.getStatus().getHostIP()));
            result.put("phase", nullSafe(pod.getStatus().getPhase()));
            result.put(AbstractContainerAppsDeployer.SPRING_APP_KEY.replace('-', '.'),
                    pod.getMetadata().getLabels().get(AbstractContainerAppsDeployer.SPRING_APP_KEY));
            result.put(AbstractContainerAppsDeployer.SPRING_DEPLOYMENT_KEY.replace('-', '.'),
                    pod.getMetadata().getLabels().get(AbstractContainerAppsDeployer.SPRING_DEPLOYMENT_KEY));
            result.put("guid", pod.getMetadata().getUid());
        } else {
            logger.debug("getAttributes:no pod");
        }
        if (service != null) {
            result.put("service.name", service.getMetadata().getName());
            if ("LoadBalancer".equals(service.getSpec().getType())) {
                if (service.getStatus() != null && service.getStatus().getLoadBalancer() != null
                        && service.getStatus().getLoadBalancer().getIngress() != null && !service.getStatus()
                        .getLoadBalancer().getIngress().isEmpty()) {
                    String externalIp = service.getStatus().getLoadBalancer().getIngress().get(0).getIp();
                    if (externalIp == null) {
                        externalIp = service.getStatus().getLoadBalancer().getIngress().get(0).getHostname();
                    }
                    result.put("service.external.ip", externalIp);
                    List<ServicePort> ports = service.getSpec().getPorts();
                    int port = 0;
                    if (ports != null && ports.size() > 0) {
                        port = ports.get(0).getPort();
                        result.put("service.external.port", String.valueOf(port));
                    }
                    if (externalIp != null) {
                        result.put("url", "http://" + externalIp + (port > 0 && port != 80 ? ":" + port : ""));
                    }

                }
            }
        } else {
            logger.debug("getAttributes:no service");
        }
        if (containerStatus != null) {
            result.put("container.restartCount", "" + containerStatus.getRestartCount());
            if (containerStatus.getLastState() != null && containerStatus.getLastState().getTerminated() != null) {
                result.put("container.lastState.terminated.exitCode",
                        "" + containerStatus.getLastState().getTerminated().getExitCode());
                result.put("container.lastState.terminated.reason",
                        containerStatus.getLastState().getTerminated().getReason());
            }
            if (containerStatus.getState() != null && containerStatus.getState().getTerminated() != null) {
                result.put("container.state.terminated.exitCode",
                        "" + containerStatus.getState().getTerminated().getExitCode());
                result.put("container.state.terminated.reason", containerStatus.getState().getTerminated().getReason());
            }
        } else {
            logger.debug("getAttributes:no containerStatus");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getAttributes:" + result);
        }
        return result;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String determineActuatorPathFromLivenessProbe(Pod pod) {
        return pod.getSpec().getContainers().stream()
                .filter((Container container) -> container.getLivenessProbe() != null &&
                        container.getLivenessProbe().getHttpGet() != null)
                .findFirst()
                .map(container ->
                        Paths.get(container.getLivenessProbe().getHttpGet().getPath()).getParent().toString())
                .orElse("/actuator");
    }

    private String determineActuatorPortFromLivenessProbe(Pod pod, String path) {
        IntOrString intOrString = pod.getSpec().getContainers().stream()
                .filter(container -> container.getLivenessProbe() != null &&
                        container.getLivenessProbe().getHttpGet() != null &&
                        container.getLivenessProbe().getHttpGet().getPath().equals(path))
                .findFirst()
                .map(container -> container.getLivenessProbe().getHttpGet().getPort())
                .orElse(new IntOrString(8080));
        return intOrString.getIntVal() != null ? String.valueOf(intOrString.getIntVal()) : intOrString.getStrVal();
    }
}
