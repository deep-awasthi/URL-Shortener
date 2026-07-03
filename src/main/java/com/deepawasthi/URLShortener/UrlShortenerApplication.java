package com.deepawasthi.URLShortener;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

@SpringBootApplication
public class UrlShortenerApplication {

	private final DataSource dataSource;

	@Value("${spring.flyway.locations:classpath:db/migration}")
	private String locations;

	public UrlShortenerApplication(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@PostConstruct
	public void runFlywayMigrations() {
		Flyway flyway = Flyway.configure()
				.dataSource(dataSource)
				.locations(locations)
				.schemas("public")
				.load();
		flyway.migrate();
	}

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerApplication.class, args);
	}
}
