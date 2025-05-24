package br.com.estapar.parkingmanagement.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueResponseDTO {
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
}
