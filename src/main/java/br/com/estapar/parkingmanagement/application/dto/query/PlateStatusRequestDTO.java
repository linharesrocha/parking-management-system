package br.com.estapar.parkingmanagement.application.dto.query;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlateStatusRequestDTO {

    @NotBlank(message = "A placa não pode estar em branco.")
    private String licensePlate;
}
