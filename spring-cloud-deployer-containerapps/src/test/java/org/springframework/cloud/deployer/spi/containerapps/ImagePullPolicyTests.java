package org.springframework.cloud.deployer.spi.containerapps;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ImagePullPolicy}.
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Moritz Schulze
 */
public class ImagePullPolicyTests {

	@Test
	public void relaxedValueOf_ignoresCase() throws Exception {
		ImagePullPolicy pullPolicy = ImagePullPolicy.relaxedValueOf("aLWays");
		assertThat(pullPolicy).isEqualTo(ImagePullPolicy.Always);
	}

	@Test
	public void relaxedValueOf_parsesValueWithDashesInsteadOfCamelCase() throws Exception {
		ImagePullPolicy pullPolicy = ImagePullPolicy.relaxedValueOf("if-not-present");
		assertThat(pullPolicy).isEqualTo(ImagePullPolicy.IfNotPresent);
	}

	@Test
	public void relaxedValueOf_returnsNullIfValueNotParseable() throws Exception {
		ImagePullPolicy pullPolicy = ImagePullPolicy.relaxedValueOf("not-a-real-policy");
		assertThat(pullPolicy).isNull();
	}
}
