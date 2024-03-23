package fink.demo.smallryeconfigspringboot;

import com.google.common.base.Verify;
import io.smallrye.config.SmallRyeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 Simple Spring Rest Controller

 {@code
 curl localhost:8080/key/smallrye.config.profile
 }
 */
@Slf4j
@RestController
public class BasicController implements InitializingBean {

	@Autowired // available as spring bean
	private SmallRyeConfig cfg;

	@Value("${example.key1}") // comes from SmallRyeConfig META-INF/microprofile-config.properties
	int keyFromMicroprofileConfigProperties;

	@Override
	public void afterPropertiesSet () {
		Verify.verify(keyFromMicroprofileConfigProperties == cfg.getValue("example.key1", int.class));
		Verify.verify(123 == cfg.getValue("example.key1", int.class));
		LOGGER.info("🔥 BasicController is alive with key={}", keyFromMicroprofileConfigProperties);
		LOGGER.info("Try yourself: curl localhost:8080/key/example.key1\n");
	}

	@GetMapping("/key/{propertyName}")
	public String showProperty (@PathVariable String propertyName) {
		try {
			return cfg.getValue(propertyName, String.class);
		} catch (Exception e) {
			return e.toString();
		}
	}

}