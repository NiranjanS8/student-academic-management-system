package com.example.sams;

import com.example.sams.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class StudentAcademicManagementSystemApplication {

    public static void main(String[] args) {
        EnvLoader.load(".env");
        SpringApplication.run(StudentAcademicManagementSystemApplication.class, args);
    }
}
