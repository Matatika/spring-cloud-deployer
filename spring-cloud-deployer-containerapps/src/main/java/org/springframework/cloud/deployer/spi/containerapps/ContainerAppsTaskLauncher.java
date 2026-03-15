package org.springframework.cloud.deployer.spi.containerapps;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.containerapps.support.PropertyParserUtils;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.monitor.query.LogsQueryClient;
import com.azure.monitor.query.models.LogsQueryResult;
import com.azure.monitor.query.models.LogsTableRow;
import com.azure.monitor.query.models.QueryTimeInterval;
import com.azure.resourcemanager.appcontainers.fluent.ContainerAppsApiClient;
import com.azure.resourcemanager.appcontainers.fluent.models.JobExecutionInner;
import com.azure.resourcemanager.appcontainers.fluent.models.JobInner;
import com.azure.resourcemanager.appcontainers.models.DefaultErrorResponseErrorException;
import com.azure.resourcemanager.appcontainers.models.JobExecutionRunningState;
import com.azure.resourcemanager.appcontainers.models.JobProvisioningState;

/**
 * A task launcher that targets Azure Container Apps.
 *
 * @author Aaron Phethean
 */
public class ContainerAppsTaskLauncher extends AbstractContainerAppsDeployer implements TaskLauncher {
	private ContainerAppsTaskLauncherProperties taskLauncherProperties;

	private static enum LogDestination {
		LOG_ANALYTICS,
		AZURE_MONITOR,
		NONE;

		public static LogDestination fromValue(String value) {
			return LogDestination.valueOf(value.toUpperCase().replace("-", "_"));
		}
	}

	private final static int MAX_OPERATION_DURATION = 45000;
    private final static Duration POLL_INTERVAL = Duration.ofSeconds(1);

	@Autowired
	public ContainerAppsTaskLauncher(ContainerAppsDeployerProperties properties,
			ContainerAppsApiClient client) {
		this(properties, new ContainerAppsTaskLauncherProperties(), client, null, new DefaultContainerFactory(properties));
	}

	@Autowired
	public ContainerAppsTaskLauncher(ContainerAppsDeployerProperties properties,
			ContainerAppsApiClient client, 
			ContainerFactory containerFactory) {
		this(properties, new ContainerAppsTaskLauncherProperties(), client, null, containerFactory);
	}

	@Autowired
	public ContainerAppsTaskLauncher(ContainerAppsDeployerProperties deployerProperties,
			ContainerAppsTaskLauncherProperties taskLauncherProperties, 
			ContainerAppsApiClient client) {
		this(deployerProperties, taskLauncherProperties, client, null, new DefaultContainerFactory(deployerProperties));
	}

	@Autowired
	public ContainerAppsTaskLauncher(ContainerAppsDeployerProperties deployerProperties,
			ContainerAppsTaskLauncherProperties taskLauncherProperties,
			ContainerAppsApiClient client, 
			LogsQueryClient logsQueryClient,
			ContainerFactory containerFactory) {
		this.properties = deployerProperties;
		this.taskLauncherProperties = taskLauncherProperties;
		this.client = client;
		this.logsQueryClient = logsQueryClient;
		this.containerFactory = containerFactory;
		this.deploymentPropertiesResolver = new DeploymentPropertiesResolver(
				ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX, properties);
		this.managedEnvironment = client.getManagedEnvironments()
				.getByResourceGroup(properties.getResourceGroup(), properties.getEnvironment());
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		String deploymentId = createDeploymentId(request);
		TaskStatus status = status(deploymentId);

		if (!status.getState().equals(LaunchState.unknown)) {
			throw new IllegalStateException("Task " + deploymentId + " already exists with a state of " + status);
		}

		if (this.maxConcurrentExecutionsReached()) {
			throw new IllegalStateException(
				String.format("Cannot launch task %s. The maximum concurrent task executions is at its limit [%d].",
					request.getDefinition().getName(), this.getMaximumConcurrentTasks())
			);
		}

		logPossibleDownloadResourceMessage(request.getResource());
		try {
			launch(deploymentId, request);
			return deploymentId;
		} catch (DefaultErrorResponseErrorException e) {
			// make these exceptions quieter as they are typically application configuration errors
			logger.error(e.getMessage());
			throw e;
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public void cancel(String deploymentId) {
		logger.debug(String.format("Cancelling task: %s", deploymentId));
		Optional<JobExecutionInner> jobExecution = client.getJobsExecutions()
				.list(properties.getResourceGroup(), deploymentId)
				.stream()
				.findFirst();
		if (jobExecution.isPresent()) {
			waitForOperation("beginStopExecution", 
					client.getJobs().beginStopExecution(properties.getResourceGroup(), deploymentId, jobExecution.get().name()));
		}
	}

	/*
	 * Cleanup resources associated with the supplied 'deploymentId' (returned from launch).
	 * NB - This is the Container App Job name.
	 */
	@Override
	public void cleanup(String deploymentId) {
		if (!client.getJobs()
			.listByResourceGroup(properties.getResourceGroup())
			.stream()
			.anyMatch(j -> deploymentId != null && deploymentId.equals(j.name()))) {
			logger.warn(String.format("Cannot delete Job for task \"%s\" (reason: Job does not exist)", deploymentId));
		}
		logger.debug(String.format("Deleting Job for task: %s", deploymentId));
		waitForOperation("beginDelete", 
				client.getJobs().beginDelete(properties.getResourceGroup(), deploymentId));
	}

	/*
	 * 'taskName' as supplied in launch request. 
	 */
	@Override
	public void destroy(String taskName) {
		for (String deploymentId : getIdsForTasks(Optional.of(taskName), properties.isCreateJob())) {
			cleanup(deploymentId);
		}
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return super.createRuntimeEnvironmentInfo(TaskLauncher.class, this.getClass());
	}

	@Override
	public TaskStatus status(String deploymentId) {
		TaskStatus status = buildTaskStatus(deploymentId);
		logger.debug(String.format("Status for task: %s is %s", deploymentId, status));
		return status;
	}

	@Override
	public int getMaximumConcurrentTasks() {
		return this.properties.getMaximumConcurrentTasks();
	}

	@Override
	public int getRunningTaskExecutionCount() {
		List<String> taskIds = getIdsForTasks(Optional.empty(), false);
		AtomicInteger executionCount = new AtomicInteger();

		taskIds.forEach(id-> {
			if (buildJobStatus(id).getState() == LaunchState.running) {
				executionCount.incrementAndGet();
			}
		});

		return executionCount.get();
	}

	/**
	 * Return the log of the task launched with the supplied 'deploymentId' (returned from launch).
	 * NB - This is the Container App Job name.
	 *
	 * @return the task application log
	 */
	@Override
	public String getLog(String deploymentId) {
		if (logsQueryClient == null) {
			logger.warn(String.format("Cannot get log for task \"%s\" (reason: log analytics client not found)", deploymentId));
			return null;
		}

		final String logAnalyticsWorkspaceId = resolveLogAnalyticsWorkspaceId();

		if (logAnalyticsWorkspaceId == null) {
			logger.warn(String.format("Cannot get log for task \"%s\" (reason: log analytics workspace id not configured)", deploymentId));
			return null;
		}

		final LogDestination logDestination = LogDestination.fromValue(managedEnvironment.appLogsConfiguration().destination());
		final String query = getQuery(logDestination, deploymentId);

		if (query == null) {
			logger.warn(String.format("No defined query for destination: %s", logDestination));
			return null;
		}

		final LogsQueryResult queryResults = logsQueryClient.queryWorkspace(
				logAnalyticsWorkspaceId,
				query,
				QueryTimeInterval.ALL);

		final StringBuilder logAppender = new StringBuilder();

		for (LogsTableRow row : queryResults.getTable().getRows()) {
			logAppender.append(row.getRow().get(0).getValueAsString());  // first cell should be log
			logAppender.append("\r");
		}

		return logAppender.toString();
	}

	private boolean maxConcurrentExecutionsReached() {
		return this.getRunningTaskExecutionCount() >= this.getMaximumConcurrentTasks();
	}

	protected String createDeploymentId(AppDeploymentRequest request) {
		String name = request.getDefinition().getName();
		Hashids hashids = new Hashids(name, 0, "abcdefghijklmnopqrstuvwxyz");
		long idToEncode = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
		String hashid = hashids.encode(idToEncode);
		String deploymentId = hashid + "-" + name;
		/*
		 * Container App names must consist of lower case alphanumeric characters or '-', 
		 * start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. 
		 * The length must not be more than 32 characters.
		 */
		return deploymentId.replace('.', '-').toLowerCase().substring(0, 32);
	}

	private void launch(String deploymentId, AppDeploymentRequest request) {
		Map<String, String> labelMap = new HashMap<>();
		labelMap.put("task-name", request.getDefinition().getName());
		labelMap.put(SPRING_MARKER_KEY, SPRING_MARKER_VALUE);

		Map<String, String> deploymentProperties = request.getDeploymentProperties();
		Map<String, String> deploymentLabels = this.deploymentPropertiesResolver.getDeploymentLabels(deploymentProperties);
		if (!CollectionUtils.isEmpty(deploymentLabels)) {
			logger.debug(String.format("Adding deploymentLabels: %s", deploymentLabels));
			labelMap.putAll(deploymentLabels);
		}

		JobInner job = createJobInner(request);
		job.withTags(labelMap);
		logger.debug(String.format("Launching Job for task: %s", deploymentId));

		// Create the job
		waitForOperation("beginCreateOrUpdate",
				client.getJobs().beginCreateOrUpdate(properties.getResourceGroup(), deploymentId, job));

		// Start the job
		waitForOperation("beginStart", 
				client.getJobs().beginStart(properties.getResourceGroup(), deploymentId));
	}

	private void waitForOperation(String operation, SyncPoller<?, ?> poller) {
		// https://learn.microsoft.com/en-us/azure/developer/java/sdk/lro
		try {
			PollResponse<?> response;
			long start = System.currentTimeMillis();
			boolean timeout = false;
			do {
			    response = poller.poll();
			    logger.info(String.format("Status of %s operation: %s", operation, response.getStatus()));
			    TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL.toMillis());
			    timeout = (System.currentTimeMillis() - start) > MAX_OPERATION_DURATION;
			} while (!response.getStatus().isComplete() && !timeout);
			if (timeout) {
				throw new RuntimeException(String.format("%s operation timeout after %sms", operation, MAX_OPERATION_DURATION));
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
			return;
		}
	}

	private List<String> getIdsForTasks(Optional<String> taskName, boolean isCreateJob) {
		// isCreateJob  'Manual' or 'Scheduled'
		List<String> ids = client.getJobs()
				.listByResourceGroup(properties.getResourceGroup())
				.stream()
				.filter(job -> job.tags() != null && job.tags().containsKey(SPRING_MARKER_KEY))
				.filter(job -> {
					return taskName.isPresent() 
							&& job.tags() != null
							&& job.tags().get("task-name") != null
							&& job.tags().get("task-name").equals(taskName.get());
				})
				.map(a -> a.name())
				.collect(Collectors.toList());
		return ids;
	}

	TaskStatus buildTaskStatus(String deploymentId) {
		if(properties.isCreateJob()) {
			throw new RuntimeException("Not implemented");
		} else {
			return buildJobStatus(deploymentId);
		}
	}

	private TaskStatus buildJobStatus(String deploymentId) {
		JobInner job = getJobByDeploymentId(deploymentId);
		if (job == null) {
			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
		}

		/*
		 * https://learn.microsoft.com/en-us/rest/api/containerapps/jobs/get?view=rest-containerapps-2024-03-01&tabs=HTTP#jobprovisioningstate
		 */
		JobProvisioningState provisioningState = job.provisioningState();
		if (provisioningState == null) {
			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
		}
		switch (provisioningState.toString()) {
		case "Failed":
			return new TaskStatus(deploymentId, LaunchState.failed, new HashMap<>());
		case "Canceled":
			return new TaskStatus(deploymentId, LaunchState.cancelled, new HashMap<>());
		case "Deleting":
			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
		case "InProgress":
			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
		case "Succeeded":
		}

		/*
		 * https://learn.microsoft.com/en-us/rest/api/containerapps/job-execution/job-execution?view=rest-containerapps-2024-03-01&tabs=HTTP#jobexecutionrunningstate
		 */
		Optional<JobExecutionInner> jobExecution = client.getJobsExecutions()
			.list(properties.getResourceGroup(), deploymentId)
			.stream()
			.findFirst();
		if (!jobExecution.isPresent()) {
			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
		}
		JobExecutionRunningState runningState = jobExecution.get().status();
		if (runningState == null) {
			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
		}
		switch (runningState.toString()) {
		case "Processing":
			return new TaskStatus(deploymentId, LaunchState.launching, new HashMap<>());
		case "Degraded":
			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
		case "Failed":
			return new TaskStatus(deploymentId, LaunchState.failed, new HashMap<>());
		case "Stopped":
			return new TaskStatus(deploymentId, LaunchState.cancelled, new HashMap<>());
		case "Succeeded":
			return new TaskStatus(deploymentId, LaunchState.complete, new HashMap<>());
		case "Running":
			return new TaskStatus(deploymentId, LaunchState.running, new HashMap<>());
		default:
			return new TaskStatus(deploymentId, LaunchState.unknown, new HashMap<>());
		}

	}

	/*
	 * deploymentId is our internal name we generated for this Container App
	 */
	private JobInner getJobByDeploymentId(String deploymentId) {
		Optional<JobInner> job = client.getJobs()
			.listByResourceGroup(properties.getResourceGroup())
			.stream()
			.filter(j -> j.name().equals(deploymentId))
			.findFirst();
		if (!job.isPresent()) {
			logger.debug(String.format("No active Job for task \"%s\"", deploymentId));
			return null;
		}
		return job.get();
	}

	/**
	 * Get the RestartPolicy setting for the deployment request.
	 *
	 * @param request The deployment request.
	 * @return Whether RestartPolicy is requested
	 */
	protected RestartPolicy getRestartPolicy(AppDeploymentRequest request) {
		String restartPolicyString =
				PropertyParserUtils.getDeploymentPropertyValue(request.getDeploymentProperties(),
						ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX+".restartPolicy");
		RestartPolicy restartPolicy =  (!StringUtils.hasText(restartPolicyString)) ? this.taskLauncherProperties.getRestartPolicy() :
				RestartPolicy.valueOf(restartPolicyString);
		if (this.properties.isCreateJob()) {
			Assert.isTrue(!restartPolicy.equals(RestartPolicy.Always), "RestartPolicy should not be 'Always' when the JobSpec is used.");
		}
		return restartPolicy;
	}

	/**
	 * Get the BackoffLimit setting for the deployment request.
	 *
	 * @param request The deployment request.
	 * @return the backoffLimit
	 */
	protected Integer getBackoffLimit(AppDeploymentRequest request) {
		String backoffLimitString = PropertyParserUtils.getDeploymentPropertyValue(request.getDeploymentProperties(),
				"spring.cloud.deployer.kubernetes.backoffLimit");
		if (StringUtils.hasText(backoffLimitString)) {
			return Integer.valueOf(backoffLimitString);
		}
		else {
			return this.taskLauncherProperties.getBackoffLimit();
		}
	}

	/**
	 * Get the ttlSecondsAfterFinihsed setting for the deployment request.
	 *
	 * @param request The deployment request.
	 * @return the ttlSecondsAfterFinished
	 */
	protected Integer getTtlSecondsAfterFinished(AppDeploymentRequest request) {
		String ttlSecondsAfterFinished = PropertyParserUtils.getDeploymentPropertyValue(request.getDeploymentProperties(),
				ContainerAppsDeployerProperties.CONTAINER_APPS_DEPLOYER_PROPERTIES_PREFIX+".ttlSecondsAfterFinished");
		if (StringUtils.hasText(ttlSecondsAfterFinished)) {
			return Integer.valueOf(ttlSecondsAfterFinished);
		}
		else {
			return this.taskLauncherProperties.getTtlSecondsAfterFinished();
		}
	}

	private static String getQuery(final LogDestination destination, final String jobName) {
		if (destination == LogDestination.AZURE_MONITOR)
			return String.format(
					"ContainerAppConsoleLogs" +
					"| where JobName == '%s'" +
					"| order by TimeGenerated asc" +
					"| project Log",
					jobName);

		if (destination == LogDestination.LOG_ANALYTICS)
			return String.format(
					"ContainerAppConsoleLogs_CL" +
					"| where ContainerJobName_s == '%s'" +
					"| order by TimeGenerated asc" +
					"| project Log_s",
					jobName);

		return null;
	}

	private String resolveLogAnalyticsWorkspaceId() {
		final String logAnalyticsWorkspaceId = properties.getLogAnalyticsWorkspaceId();

		return logAnalyticsWorkspaceId != null
				? logAnalyticsWorkspaceId
				: managedEnvironment.appLogsConfiguration().logAnalyticsConfiguration().customerId();
	}

}
