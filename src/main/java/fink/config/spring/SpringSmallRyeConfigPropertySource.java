package fink.config.spring;

import com.google.common.collect.Iterables;
import io.smallrye.config.SmallRyeConfig;
import lombok.NonNull;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 Spring {@link PropertySource} for Environment to get access to {@link SmallRyeConfig}/{@link Config}

 @see org.springframework.core.env.Environment
 */
public class SpringSmallRyeConfigPropertySource extends EnumerablePropertySource<Config> {
	public static final String NAME = "SmallRyeConfig";

	public SpringSmallRyeConfigPropertySource (@NonNull Config config) {
		super(NAME, config);
	}//new

	/**
	 ~ {@link SmallRyeConfig#getRawValue(String)} ~ Not really raw (but before expressions?)
	 */
	@Override  @Nullable
	public String getProperty (@Nullable String key) {
		if (key == null){
			return null;// ok?
		}
		ConfigValue configValue = source.getConfigValue(key);
		String value = configValue == null
				? null
				: configValue.getValue();
		return value;
	}

	@Override
	public boolean containsProperty (@Nullable String key) {
		return getProperty(key) != null;
	}

	@Override  @Nonnull
	public String[] getPropertyNames () {
		return Iterables.toArray(source.getPropertyNames(), String.class);
	}
}