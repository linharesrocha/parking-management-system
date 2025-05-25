package br.com.estapar.parkingmanagement.application.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para solicitar o status de um veículo pela placa.")
public class PlateStatusRequestDTO {

    @NotBlank(message = "A placa não pode estar em branco.")
    @Schema(description = "Placa do veículo a ser consultada.", example = "BRA2E19")
    private String licensePlate;
}
