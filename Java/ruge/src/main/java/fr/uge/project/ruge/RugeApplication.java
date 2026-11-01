package fr.uge.project.ruge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RugeApplication {
	public static void main(String[] args) {
		SpringApplication.run(RugeApplication.class, args);
	}
}
