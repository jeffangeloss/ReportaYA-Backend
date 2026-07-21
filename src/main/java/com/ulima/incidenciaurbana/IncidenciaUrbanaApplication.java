package com.ulima.incidenciaurbana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IncidenciaUrbanaApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidenciaUrbanaApplication.class, args);
    }

}
