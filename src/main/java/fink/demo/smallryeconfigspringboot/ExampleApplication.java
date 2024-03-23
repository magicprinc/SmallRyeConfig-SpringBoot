package fink.demo.smallryeconfigspringboot;

import com.google.common.base.Verify;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApplication {

  public static void main(String[] args) {
		// ignore spring, use directly, to show: the problem is not in the spring, but in spring-boot.jar
		Verify.verify(SmallRyeConfigAutoConf.smallRyeConfig().getValue("vaadin.launch-browser", boolean.class));
		Verify.verify(SmallRyeConfigAutoConf.smallRyeConfig().getValue("demo.mode.enabled", int.class) == 12345);
		Verify.verify(SmallRyeConfigAutoConf.smallRyeConfig().getValue("example.key1", int.class) == 123);
		Verify.verify(SmallRyeConfigAutoConf.smallRyeConfig().getValue("yaml.here", Boolean.class));

    SpringApplication.run(ExampleApplication.class, args);
  }
}