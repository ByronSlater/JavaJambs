package com.javajambs.cher.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String PROPERTY_SOURCE_NAME = "dotenv";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String dotenvFile = environment.getProperty("dotenv.file",
				environment.getProperty("DOTENV_FILE", ".env"));

		Dotenv dotenv = Dotenv.configure()
				.filename(dotenvFile)
				.ignoreIfMissing()
				.load();

		Map<String, Object> properties = new LinkedHashMap<>();
		dotenv.entries().forEach(entry -> properties.put(
				entry.getKey().toLowerCase(Locale.ROOT).replace('_', '.'),
				entry.getValue()));

		if (!properties.isEmpty()) {
			environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
		}
	}

}
