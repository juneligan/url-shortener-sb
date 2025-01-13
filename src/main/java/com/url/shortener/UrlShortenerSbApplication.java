package com.url.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.url.shortener.entity", "com.auth.user.entity"})
@ComponentScan(basePackages = {"com.url.shortener", "com.auth.user"})
@EnableJpaRepositories(basePackages = {"com.url.shortener.repository", "com.auth.user.repository"})
@SpringBootApplication
public class UrlShortenerSbApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerSbApplication.class, args);
	}

}
