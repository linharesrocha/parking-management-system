package br.com.estapar.parkingmanagement.application.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO contendo o status atual de um veículo no estacionamento.")
public class PlateStatusResponseDTO {

    @Schema(description = "Placa do veículo consultado.", example = "BRA2E19")
    private String licensePlate;

    @Schema(description = "Preço acumulado até o momento da consulta.", example = "15.50")
    private BigDecimal priceUntilNow;

    @Schema(description = "Data e hora de entrada do veículo no formato ISO.", example = "2025-01-01T12:00:00")
    private String entryTime;

    @Schema(description = "Tempo que o veículo permaneceu estacionado até o momento, no formato ISO 8601 de duração.", example = "PT2H30M")
    private String timeParked;

    @Schema(description = "Latitude da vaga onde o veículo está estacionado.", example = "-23.561684")
    private Double lat;

    @Schema(description = "Longitude da vaga onde o veículo está estacionado.", example = "-46.655981")
    private Double lng;
}
