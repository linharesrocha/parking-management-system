package br.com.estapar.parkingmanagement.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateStatusResponseDTO {

    private String licensePlate;
    private BigDecimal priceUntilNow;
    private String entryTime;
    private String timeParked;
    private Double lat;
    private Double lng;
}
