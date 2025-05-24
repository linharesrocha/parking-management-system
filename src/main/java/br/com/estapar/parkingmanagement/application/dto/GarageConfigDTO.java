package br.com.estapar.parkingmanagement.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GarageConfigDTO {

    @JsonProperty("garage")
    private List<SectorDTO> sectors;

    @JsonProperty("spots")
    private List<SpotDTO> spots;
}
