package fink.config.spring;

import com.google.common.base.Splitter;
import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.PropertiesConfigSourceProvider;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSourceProvider;
import io.smallrye.config.validator.BeanValidationConfigValidatorImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 1. Register "smallRyeConfig" bean (it can be injected)
 2. Add SpringSmallRyeConfigPropertySource to all ConfigurableEnvironment in Spring Context
 		(All SmallRyeConfig properties are accessible trough Environment and @Value)

 For raw/core Spring: register this class as bean "manually"

 @see SmallRyeConfig
 @see SmallRyeConfigBuilder
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
public class SmallRyeConfigAutoConf implements PriorityOrdered, BeanFactoryPostProcessor {
	@Override
	public int getOrder () {
		return HIGHEST_PRECEDENCE;
	}

	private static final SmallRyeConfig config;
	static {
		SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
				.addDefaultInterceptors()// e.g. io.smallrye.config.ExpressionConfigSourceInterceptor
				.addDefaultSources()// EnvConfigSource, SysPropConfigSource, SmallRyeConfigBuilder#META_INF_MICROPROFILE_CONFIG_PROPERTIES = META-INF/microprofile-config.properties

				.addDiscoveredConverters()
				.addDiscoveredInterceptors()
				.addDiscoveredSources()
				.addDiscoveredSecretKeysHandlers()
				//.addDiscoveredValidator() → we explicitly expose ourselves by checking what is being created (there are dependencies)
				.addDiscoveredCustomizers()
				.withSources(new DotEnvConfigSourceProvider());// (295) .env file in the current working directory

		builder
				// YAML (supports json with .yaml extension)
				.withSources(new YamlConfigSourceProvider(){// (110)
					@Override public Iterable<ConfigSource> getConfigSources (ClassLoader cl){
						var sources = new ArrayList<ConfigSource>(8);
						sources.addAll(loadConfigSources(new String[]{"config/application.yaml", "config/application.yml"}, 266, cl));
						sources.addAll(loadConfigSources(new String[]{"application.yaml", "application.yml"}, 256, cl));
						sources.addAll(loadConfigSources(new String[]{"application-test.yaml", "application-test.yml"}, 316, cl));
						return sources;
					}
					@Override public String toString (){ return "YamlConfigSourceProvider:"+getConfigSources(getClassLoader()); }
				})
				.withSources(new PropertiesConfigSourceProvider("application.properties", getClassLoader(), true))

				.withConverter(DateFormat.class, 101, SimpleDateFormat::new)
				.withConverter(DateTimeFormatter.class, 101, DateTimeFormatter::ofPattern)
				.withConverter(CharSequence.class, 101, Object::toString)
				.withConverter(Charset.class, 101, Charset::forName)
				.withConverter(Locale.class, 101, s->{
					if (s == null || s.isBlank()){
						return null;
					} else if (s.indexOf('-') >= 0){
						return Locale.forLanguageTag(s.trim().strip());// ru-RU
					} else {
						List<String> elements = Splitter.on('_').limit(3).trimResults().splitToList(s);
						return elements.size() == 1 ? new Locale(elements.get(0))
								: elements.size() == 2 ? new Locale(elements.get(0), elements.get(1))
								: new Locale(elements.get(0), elements.get(1), elements.get(2));
					}
				})
				.withValidateUnknown(false)// allow extra unmapped properties
		//.withMappingDefaults(true) ??? 👨‍💻
		;

		try {
			var validator = new BeanValidationConfigValidatorImpl();
			builder.withValidator(validator);
		} catch (Throwable e){
			// jakarta.validation.NoProviderFoundException: Unable to create a Configuration, because no Jakarta Bean Validation provider could be found. Add a provider like Hibernate Validator (RI) to your classpath.
			// NoClassDefFoundError: jakarta/validation/Validation
			LOGGER.info("microprofile-config: No BeanValidation available in ClassPath: {}", e.toString());
		}
		config = builder.build();
	}

	@Bean
	@Primary
	public static SmallRyeConfig smallRyeConfig (){
		return config;
	}

	public static ClassLoader getClassLoader (){
		var cl = Thread.currentThread().getContextClassLoader();
		return cl != null ? cl    // <^ ~ Objects.requireNonNullElseGet
				: SmallRyeConfigAutoConf.class.getClassLoader();
	}

	/**
	 Spring is huge. If you know a better way for raw/core Spring, please, open an issue or send me a message. 🙏

	 For Spring Boot there is {@link org.springframework.boot.env.EnvironmentPostProcessor}
	 */
	@Override
	public void postProcessBeanFactory (ConfigurableListableBeanFactory beanFactory) throws BeansException {
		var ps = new SpringSmallRyeConfigPropertySource(config);

		Map<String,ConfigurableEnvironment> envs = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, ConfigurableEnvironment.class);
		for (var env : envs.values()) {
			if (!env.getPropertySources().contains(SpringSmallRyeConfigPropertySource.NAME)) {
				env.getPropertySources()
						.addLast(ps);
				LOGGER.debug("ConfigurableEnvironment with SpringSmallRyeConfigPropertySource: {}", env);
			}
		}
	}
}