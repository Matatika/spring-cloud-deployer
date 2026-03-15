package org.springframework.cloud.deployer.spi.containerapps.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for formatting and parsing properties
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Chris Schaefer
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public class PropertyParserUtils {
	/**
	 * Extracts annotations from the provided value
	 *
	 * @param stringPairs The deployment request annotations
	 * @return {@link Map} of annotations
	 */
	public static Map<String, String> getStringPairsToMap(String stringPairs) {
		Map<String, String> mapValue = new HashMap<>();

		if (StringUtils.hasText(stringPairs)) {
			/**
			 * Positive look ahead that into a non capturing group that will skip all commas in quotes.
			 * Even number quotes will be ignored by the non capturing group.
			 */
			String[] pairs = stringPairs.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
			for (String pair : pairs) {
				String[] splitString = pair.split(":", 2);
				Assert.isTrue(splitString.length == 2, String.format("Invalid annotation value: %s", pair));
				String value = splitString[1].trim();
				mapValue.put(splitString[0].trim(), value);
			}
		}
		return mapValue;
	}

	public static String getDeploymentPropertyValue(Map<String, String> deploymentProperties, String propertyName) {
		return getDeploymentPropertyValue(deploymentProperties, propertyName, null);
	}

	public static String getDeploymentPropertyValue(Map<String, String> deploymentProperties, String propertyName,
			String defaultValue) {
		RelaxedNames relaxedNames = new RelaxedNames(propertyName);
		for (Iterator<String> itr = relaxedNames.iterator(); itr.hasNext();) {
			String relaxedName = itr.next();
			if (deploymentProperties.containsKey(relaxedName)) {
				return deploymentProperties.get(relaxedName);
			}
		}
		return defaultValue;
	}
}
