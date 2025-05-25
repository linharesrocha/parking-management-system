package br.com.estapar.parkingmanagement.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotDTO {

    @JsonProperty("id")
    private Long externalId;

    @JsonProperty("sector")
    private String sectorName;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lng")
    private Double lng;

    @JsonProperty("occupied")
    private boolean occupied;
}
