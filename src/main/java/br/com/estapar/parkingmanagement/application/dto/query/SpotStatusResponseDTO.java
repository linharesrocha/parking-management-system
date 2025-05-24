package br.com.estapar.parkingmanagement.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotStatusResponseDTO {

    private boolean occupied;
    private String licensePlate;
    private BigDecimal priceUntilNow;
    private String entryTime;
    private String timeParked;
}
