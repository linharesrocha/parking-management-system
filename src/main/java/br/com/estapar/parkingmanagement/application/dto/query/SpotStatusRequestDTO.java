package br.com.estapar.parkingmanagement.application.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO para solicitar o status de uma vaga específica por suas coordenadas.")
public class SpotStatusRequestDTO {

    @NotNull(message = "Latitude não pode ser nula.")
    @Schema(description = "Latitude da vaga.", example = "-23.561684", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lat;

    @NotNull(message = "Longitude não pode ser nula.")
    @Schema(description = "Longitude da vaga.", example = "-46.655981", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double lng;
}
