package org.springframework.cloud.deployer.spi.containerapps.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.azure.resourcemanager.appcontainers.fluent.models.JobExecutionInner;
import com.azure.resourcemanager.appcontainers.fluent.models.RevisionInner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContainerAppsStatusUtilTests {

	@Test
	public void testAppIsRunning() throws JsonMappingException, JsonProcessingException {
		String jsonString = "{"
				+ "  \"properties\": {"
				+ "    \"runningState\": \"Running\""
				+ "  }"
				+ "}";
		ObjectMapper objectMapper = new ObjectMapper();
		RevisionInner revision = objectMapper.readValue(jsonString, RevisionInner.class);
		assertThat(ContainerAppsStatusUtil.isRunning(revision)).isEqualTo(true);
	}

	@Test
	public void testJobIsRunning() throws JsonMappingException, JsonProcessingException {
		String jsonString = "{"
				+ "  \"properties\": {"
				+ "    \"status\": \"Running\""
				+ "  }"
				+ "}";
		ObjectMapper objectMapper = new ObjectMapper();
		JobExecutionInner jobExecution = objectMapper.readValue(jsonString, JobExecutionInner.class);
		assertThat(ContainerAppsStatusUtil.isRunning(jobExecution)).isEqualTo(true);
	}

}
