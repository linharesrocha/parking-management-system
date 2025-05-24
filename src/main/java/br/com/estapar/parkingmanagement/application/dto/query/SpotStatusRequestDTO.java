package br.com.estapar.parkingmanagement.application.dto.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotStatusRequestDTO {

    @NotNull(message = "Latitude não pode ser nula.")
    private Double lat;

    @NotNull(message = "Longitude não pode ser nula.")
    private Double lng;
}
