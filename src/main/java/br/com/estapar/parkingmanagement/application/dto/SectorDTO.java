package br.com.estapar.parkingmanagement.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SectorDTO {

    @JsonProperty("sector")
    private String name;

    @JsonProperty("base_price")
    private BigDecimal basePrice;

    @JsonProperty("max_capacity")
    private int maxCapacity;

    @JsonProperty("open_hour")
    private String openHour;

    @JsonProperty("close_hour")
    private String closeHour;

    @JsonProperty("duration_limit_minutes")
    private int durationLimitMinutes;
}
