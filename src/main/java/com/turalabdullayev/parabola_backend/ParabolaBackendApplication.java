package com.turalabdullayev.parabola_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Parabola API", version = "1.0", description = "Geyim və Ölçü Alqoritmi Sistemi"))
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class ParabolaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParabolaBackendApplication.class, args);
	}

}