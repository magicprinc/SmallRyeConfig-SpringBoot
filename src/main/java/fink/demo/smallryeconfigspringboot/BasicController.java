package fink.demo.smallryeconfigspringboot;

import io.smallrye.config.SmallRyeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 @see  */
@RestController
public class BasicController {

	@Autowired
	private SmallRyeConfig cfg;

	@GetMapping("/key/{propertyName}")
	public String showProperty (@PathVariable String propertyName) {
		try {
			return cfg.getValue(propertyName, String.class);
		} catch (Exception e) {
			return e.toString();
		}
	}

}