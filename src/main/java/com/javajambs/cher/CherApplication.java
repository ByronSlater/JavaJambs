package com.javajambs.cher;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class CherApplication {

	public static void main(String[] args) {
		String dotenvFile = System.getProperty(
				"dotenv.file",
				System.getenv().getOrDefault("DOTENV_FILE", ".env"));

		Dotenv.configure()
				.filename(dotenvFile)
				.ignoreIfMissing()
				.load()
				.entries()
				.forEach(entry -> System.setProperty(
						entry.getKey().toLowerCase(Locale.ROOT).replace('_', '.'),
						entry.getValue()));

		SpringApplication.run(CherApplication.class, args);
	}

}
