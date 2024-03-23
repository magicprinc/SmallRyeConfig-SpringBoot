package fink.demo.smallryeconfigspringboot;

import com.google.common.base.Splitter;
import io.smallrye.config.DotEnvConfigSourceProvider;
import io.smallrye.config.PropertiesConfigSourceProvider;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSourceProvider;
import io.smallrye.config.validator.BeanValidationConfigValidatorImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 @see SmallRyeConfig
 @see SmallRyeConfigBuilder
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
public class SmallRyeConfigAutoConf {

	@Bean
	@Primary
	public static SmallRyeConfig smallRyeConfig (){
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
						val sources = new ArrayList<ConfigSource>(8);
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
		return builder.build();
	}

	public static ClassLoader getClassLoader (){
		var cl = Thread.currentThread().getContextClassLoader();
		return cl != null ? cl    // <^ ~ Objects.requireNonNullElseGet
				: SmallRyeConfigAutoConf.class.getClassLoader();
	}
}