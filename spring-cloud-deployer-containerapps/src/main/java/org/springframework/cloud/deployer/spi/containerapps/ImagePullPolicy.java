package org.springframework.cloud.deployer.spi.containerapps;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * ImagePullPolicy for containers inside a Kubernetes Pod, cf. https://kubernetes.io/docs/user-guide/images/
 *
 * Based on and with thanks to the original authors org.springframework.cloud.deployer.spi.kubernetes
 * @author Moritz Schulze
 */
public enum ImagePullPolicy {

    Always,
    IfNotPresent,
    Never;

    /**
     * Tries to convert {@code name} to an {@link ImagePullPolicy} by ignoring case, dashes, underscores
     * and so on in a relaxed fashion.
     *
     * @param name The name to convert to an {@link ImagePullPolicy}.
     * @return The {@link ImagePullPolicy} for {@code name} or {@code null} if the conversion was not possible.
     */
    public static ImagePullPolicy relaxedValueOf(String name) {
		// 'value' is just a dummy key as you can't bind a single value to an enum
		Map<String, String> props = new HashMap<>();
		props.put("value", name);
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(props);
		Binder binder = new Binder(source);
		try {
			return binder.bind("value", Bindable.of(ImagePullPolicy.class)).get();
		} catch (Exception e) {
			// error means we couldn't bind, caller seem to handle null
		}
        return null;
    }

}
