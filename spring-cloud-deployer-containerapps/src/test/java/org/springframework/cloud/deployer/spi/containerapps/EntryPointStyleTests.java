package org.springframework.cloud.deployer.spi.containerapps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EntryPointStyle}
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 */
public class EntryPointStyleTests {

	@Test
	public void testInvalidEntryPointStyleDefaulting() {
		EntryPointStyle entryPointStyle = EntryPointStyle
				.relaxedValueOf("unknown");
		assertThat(entryPointStyle).isEqualTo(EntryPointStyle.exec);
	}

	@Test
	public void testMatchEntryPointStyle() {
		EntryPointStyle entryPointStyle = EntryPointStyle
				.relaxedValueOf("shell");
		assertThat(entryPointStyle).isEqualTo(EntryPointStyle.shell);
	}

	@Test
	public void testMixedCaseEntryPointStyle() {
		EntryPointStyle entryPointStyle = EntryPointStyle
				.relaxedValueOf("bOOt");
		assertThat(entryPointStyle).isEqualTo(EntryPointStyle.boot);
	}
}
