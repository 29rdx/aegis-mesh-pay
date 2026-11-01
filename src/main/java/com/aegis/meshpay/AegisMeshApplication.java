package com.aegis.meshpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AegisMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisMeshApplication.class, args);
    }
}
