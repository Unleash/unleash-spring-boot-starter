package io.getunleash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class UnleashApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext app = SpringApplication.run(UnleashApplication.class);
        System.out.println("Configured " +app.getBeanDefinitionCount() + " beans");
    }

}
