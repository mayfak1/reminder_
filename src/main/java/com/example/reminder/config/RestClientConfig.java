package com.example.reminder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class RestClientConfig {
    @Bean
    public RestClient.Builder restClientBuilder(){
        log.debug("Config: creating RestClient.Builder bean");
        return RestClient.builder();
    }
}
