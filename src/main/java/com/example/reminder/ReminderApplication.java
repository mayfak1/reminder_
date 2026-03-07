package com.example.reminder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class ReminderApplication {

	public static void main(String[] args) {
		log.info("App: starting ReminderApplication");
		SpringApplication.run(ReminderApplication.class, args);
		log.info("App: ReminderApplication started");
	}

}
