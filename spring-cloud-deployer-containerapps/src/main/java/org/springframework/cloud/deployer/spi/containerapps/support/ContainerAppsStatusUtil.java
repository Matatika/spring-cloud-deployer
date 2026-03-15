package org.springframework.cloud.deployer.spi.containerapps.support;

import com.azure.resourcemanager.appcontainers.fluent.models.JobExecutionInner;
import com.azure.resourcemanager.appcontainers.fluent.models.RevisionInner;
import com.azure.resourcemanager.appcontainers.models.JobExecutionRunningState;
import com.azure.resourcemanager.appcontainers.models.RevisionRunningState;

public class ContainerAppsStatusUtil {

   private ContainerAppsStatusUtil() {}

   /**
    * Returns {@code true} if the given App is running. Returns {@code false} otherwise.
    *
    * RUNNING means at least one container is still running or is in the process of being restarted.
    * 
    * @param app the app to return the running status for
    * @return returns true if the app is running
    */
   public static boolean isRunning(RevisionInner appRevision) {
       if (isInStatus(RevisionRunningState.RUNNING, appRevision)) {
           return true;
       }
//       if (hasDeletionTimestamp(container)
//               || isInitializing(container)) {
//           return false;
//       }
//       if (hasRunningContainer(container)) {
//           return !hasCompletedContainer(container)
//                   || Readiness.isPodReady(container);
//       }
       return false;
   }

   private static boolean isInStatus(RevisionRunningState expected, RevisionInner app) {
       if (app == null
               || app.runningState() == null
               || expected == null) {
           return false;
       }
       return expected.equals(app.runningState());
   }

   /**
    * Returns {@code true} if the given Job Execution is running. Returns {@code false} otherwise.
    *
    * RUNNING means at least one container is still running or is in the process of being restarted.
    * 
    * @param app the app to return the running status for
    * @return returns true if the app is running
    */
   public static boolean isRunning(JobExecutionInner jobExecution) {
       if (isInStatus(JobExecutionRunningState.RUNNING, jobExecution)) {
           return true;
       }
//       if (hasDeletionTimestamp(container)
//               || isInitializing(container)) {
//           return false;
//       }
//       if (hasRunningContainer(container)) {
//           return !hasCompletedContainer(container)
//                   || Readiness.isPodReady(container);
//       }
       return false;
   }

   private static boolean isInStatus(JobExecutionRunningState expected, JobExecutionInner jobExecution) {
       if (jobExecution == null
               || jobExecution.status() == null
               || expected == null) {
           return false;
       }
       return expected.equals(jobExecution.status());
   }

//   private static boolean hasDeletionTimestamp(Pod pod) {
//       if (pod == null) {
//           return false;
//       }
//       return pod.getMetadata() != null
//               && pod.getMetadata().getDeletionTimestamp() != null;
//   }
//
//   /**
//    * Returns {@code true} if the given pod has at least 1 container that's initializing.
//    *
//    * @param pod the pod to return the initializing status for
//    * @return returns true if the pod is initializing
//    */
//   public static boolean isInitializing(Pod pod) {
//       if (pod == null) {
//           return false;
//       }
//       return pod.getStatus().getInitContainerStatuses().stream()
//               .anyMatch(PodStatusUtil::isInitializing);
//   }
//
//   /**
//    * Returns {@code true} if the given container status is terminated with an non-0 exit code or is waiting.
//    * Returns {@code false} otherwise.
//    */
//   private static boolean isInitializing(ContainerStatus status) {
//       if (status == null) {
//           return true;
//       }
//
//       ContainerState state = status.getState();
//       if (state == null) {
//           return true;
//       }
//       if (isTerminated(state)) {
//           return hasNonNullExitCode(state);
//       } else if (isWaiting(state)) {
//           return !isWaitingInitializing(state);
//       } else {
//           return true;
//       }
//   }
//
//   private static boolean isTerminated(ContainerState state) {
//       return state == null
//           || state.getTerminated() != null;
//   }
//
//   private static boolean hasNonNullExitCode(ContainerState state) {
//       return state.getTerminated() != null
//               && state.getTerminated().getExitCode() != 0;
//   }
//
//   private static boolean isWaiting(ContainerState state) {
//       return state == null
//               || state.getWaiting() != null;
//   }
//
//   private static boolean isWaitingInitializing(ContainerState state) {
//       return state != null
//               && state.getWaiting() != null
//               && POD_INITIALIZING.equals(state.getWaiting().getReason());
//   }
//
//   private static boolean hasRunningContainer(Pod pod) {
//       return getContainerStatus(pod).stream()
//               .anyMatch(PodStatusUtil::isRunning);
//   }
//
//   private static boolean isRunning(ContainerStatus status) {
//       return status.getReady() != null
//               && status.getState() != null
//               && status.getState().getRunning() != null;
//   }
//
//   private static boolean hasCompletedContainer(Pod pod) {
//       return getContainerStatus(pod).stream()
//               .anyMatch(PodStatusUtil::isCompleted);
//   }
//
//   private static boolean isCompleted(ContainerStatus status) {
//       return status.getState() != null
//               && status.getState().getTerminated() != null
//               && CONTAINER_COMPLETED.equals(status.getState().getTerminated().getReason());
//   }
//
//   /**
//    * Returns the container status for all containers of the given pod. Returns an empty list
//    * if the pod has no status
//    *
//    * @param pod the pod to return the container status for
//    * @return list of container status
//    *
//    * @see Pod#getStatus()
//    * @see PodStatus#getContainerStatuses()
//    */
//   public static List<ContainerStatus> getContainerStatus(Pod pod) {
//       if (pod == null
//               || pod.getStatus() == null) {
//           return Collections.emptyList();
//       }
//       return pod.getStatus().getContainerStatuses();
//   }

}