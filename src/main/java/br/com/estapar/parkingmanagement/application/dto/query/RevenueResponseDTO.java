package br.com.estapar.parkingmanagement.application.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO contendo o faturamento total para um setor em uma data específica.")
public class RevenueResponseDTO {
    @Schema(description = "Valor total do faturamento.", example = "250.75")
    private BigDecimal amount;

    @Schema(description = "Moeda do faturamento.", example = "BRL")
    private String currency;

    @Schema(description = "Timestamp de quando o cálculo do faturamento foi realizado.", example = "2025-05-24T18:00:00.123")
    private LocalDateTime timestamp;
}
