package org.hhnotifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HhNotifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(HhNotifierApplication.class, args);
	}

}
