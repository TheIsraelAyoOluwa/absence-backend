package com.theisraelayooluwa.absencebackend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Absence Management API",
                version = "1.0",
                description = "API for managing employees, employers, absences, and entitlement calculations"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class AbsenceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbsenceBackendApplication.class, args);
    }

}
