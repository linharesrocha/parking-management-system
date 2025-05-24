package br.com.estapar.parkingmanagement.application.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO contendo o status de uma vaga específica.")
public class SpotStatusResponseDTO {

    @Schema(description = "Indica se a vaga está ocupada.", example = "true")
    private boolean occupied;

    @Schema(description = "Placa do veículo que ocupa a vaga (se estiver ocupada).", example = "BRA2E19", nullable = true)
    private String licensePlate;

    @Schema(description = "Preço acumulado para o veículo na vaga até o momento (se estiver ocupada).", example = "7.50", nullable = true)
    private BigDecimal priceUntilNow;

    @Schema(description = "Data e hora de entrada do veículo na vaga (se estiver ocupada), no formato ISO.", example = "2025-01-01T10:30:00", nullable = true)
    private String entryTime;

    @Schema(description = "Tempo que o veículo permaneceu na vaga até o momento (se estiver ocupada), no formato ISO 8601 de duração.", example = "PT1H15M", nullable = true)
    private String timeParked;
}
