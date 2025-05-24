package br.com.estapar.parkingmanagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
		title = "Estapar - Parking Management API",
		version = "v1",
		description = "API para gerenciamento de estacionamento, incluindo controle de vagas, "
		+ "entrada e saída de veículos, e faturamento."
))
public class ParkingManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParkingManagementApplication.class, args);
	}

}
